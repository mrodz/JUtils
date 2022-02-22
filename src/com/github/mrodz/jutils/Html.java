package com.github.mrodz.jutils;

import java.util.*;

public class Html {
    static class Element {
        String tagName;
        List<Element> content;

        public Element(String tagName, List<Element> content) {
            this.tagName = tagName;
            this.content = content;
        }

        @Override
        public String toString() {
            return tagName.concat(content.stream().map(Element::toString).reduce("", String::concat))
                    .concat(isHtmlTag(tagName) ? closedHtmlTagOf(tagName) : "");
        }

        /**
         * @see #size(int, Element)
         */
        public int size() {
            return size(1, this);
        }

        /**
         * Used in {@link #size()} to get the count of nodes in this tree.
         *
         * @param res  recursive parameter; set to {@code 0}.
         * @param init the {@link Element} to start at.
         * @return the count of nodes in this tree.
         */
        private int size(int res, Element init) {
            List<Element> children = init.content;

            // No children exist; the node is the last node in this lineage
            if (children.size() == 0) {
                return res;
            }

            // Go through each child
            for (int i = 0; i < children.size(); i++, res++) {
                Element node = children.get(i);

                res += size(0, node);
            }

            return res;
        }

        public Iterator<Element> htmlElementIterator() {
            var htmlElement = buildDocumentTree0(new ArrayDeque<>(), this.toString());
            final List<Element> toIterate = new ArrayList<>();
            {
                while (!htmlElement.getValue().isEmpty()) {
                    var o = htmlElement.getValue().poll();
                    toIterate.add(o);
                }
            }

            return toIterate.iterator();
        }
    }

    public static void main(String[] args) {
        {
            var s = "<html><a>hello</a><h1><h4>nestedHello</h4><h3>nestedWorld</h3><h6><br>top</br></h6></h1><br>world</br></html>";
            var htmlElement = buildDocumentTree0(new ArrayDeque<>(), s);

            System.out.println("@@ RESULTS:");
            while (!htmlElement.getValue().isEmpty()) {
                var o = htmlElement.getValue().poll().content.stream().map(Element::toString).reduce("", String::concat);
                if (!o.isEmpty()) System.out.println("@@ " + o);
            }
        }
        System.out.println(isHtmlContainedInElement("<div></div><html>Hello!<script>console.log('Hello World!');</script></html>"));
    }

    static List<String> listOfTags(String str) {
        List<String> activeTags = new ArrayList<>();
        char[] charArray = str.toCharArray();
        for (int i = 0, charArrayLength = charArray.length; i < charArrayLength; i++) {
            char c = charArray[i];
            StringBuilder tag = new StringBuilder();
            for (int j = i; j < charArrayLength; i = ++j) {
                if (c != '<' && charArray[j] == '<') {
                    i--;
                    break;
                }
                tag.append(charArray[j]);
                if (c == '<' && charArray[j] == '>') {
                    break;
                }
            }
            activeTags.add(tag.toString());
        }
        return activeTags;
    }

    /**
     * Are all the HTML tags that were opened now closed?
     */
    public static boolean isHtmlClosed(String s) {
        ArrayDeque<String> activeTags = new ArrayDeque<>();
        List<String> tags = listOfTags(s);

        for (String tag : tags) {
            String last = activeTags.peekFirst();

            if (isHtmlTag(tag)) {
                if (last != null && tag.equals(closedHtmlTagOf(last))) {
                    var popped = activeTags.pop();
                } else {
                    activeTags.push(tag);
                }
            }
        }

        System.out.println();
        System.out.println(activeTags);

        return activeTags.size() == 0;
    }

    /**
     * Is all the HTML wrapped in a single element?
     */
    public static boolean isHtmlContainedInElement(String s) {
        ArrayDeque<String> activeTags = new ArrayDeque<>();
        List<String> tags = listOfTags(s);
        if (tags.size() == 0) return false;
        String firstTag = tags.get(0);
        if (firstTag == null || !isHtmlTag(firstTag)) return false;

        activeTags.push(firstTag);
        for (int i = 1, tagsSize = tags.size(); i < tagsSize; i++) {
            String tag = tags.get(i);
            String last = activeTags.peekFirst();
            if (isHtmlTag(tag)) {
                if (last != null && tag.equals(closedHtmlTagOf(last))) {
                    var popped = activeTags.pop();
                } else {
                    activeTags.push(tag);
                }
            }
            if (activeTags.size() == 0 && i != tagsSize - 1) return false;
        }
        return true;
    }

    private static Map.Entry<List<Element>, Queue<Element>> buildDocumentTree0(Queue<Element> queues, String s) {
        List<String> activeTags = listOfTags(s);

        List<Element> children = new ArrayList<>();
        for (int i = 0; i < activeTags.size(); i++) {
            String thisElement = activeTags.get(i);
            List<String> content = new ArrayList<>();

            for (int j = ++i; j < activeTags.size(); i = ++j) {
                if (isHtmlTag(activeTags.get(j)) && activeTags.get(j).equals(closedHtmlTagOf(thisElement))) {
                    break;
                } else {
                    content.add(activeTags.get(j));
                }
            }

            String contentAsString = content.stream().reduce("", String::concat);

            Element child = new Element(thisElement, buildDocumentTree0(queues, contentAsString).getKey());
            queues.add(child);
            children.add(child);
        }

        return Map.entry(children, queues);
    }

    public static boolean isHtmlTag(String s) {
        return s.matches("<[^<>]+>");
    }

    public static String closedHtmlTagOf(String tag) {
        if (tag.matches("</[^<>]+>")) throw new IllegalArgumentException("tag (" + tag + ") is already closed");
        if (!isHtmlTag(tag)) throw new IllegalArgumentException("tag (" + tag + ") is not a valid HTML tag");

        return String.format("</%s>", tag.replaceAll("[<>]", ""));
    }
}
