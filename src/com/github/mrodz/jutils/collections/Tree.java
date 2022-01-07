package com.github.mrodz.jutils.collections;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.mrodz.jutils.Commons.*;

/**
 * <p>An implementation of a Tree Data Structure in Java. This class acts as both
 * an individual node and a tree data structure at the same time. It can serve
 * useful when establishing relational and hierarchical relationships between
 * objects, and differs from a standard Binary Tree.</p>
 *
 * <p>Below is a sample implementation.</p>
 * <p><pre>
 *     Tree&lt;String&gt; exampleTree = new Tree&lt;&gt;("Languages");
 *     exampleTree.insert("Compiled", "Interpreted", "Esoteric");
 *
 *     exampleTree.getNode(0).insert("Java", "C++", "Go");
 *     exampleTree.getNode(1).insert("Python", "JavaScript");
 *     exampleTree.getNode(2).insert("BrainF***");
 *
 *     exampleTree.getNode(0).getNode(0).insert("Java is cool", "Android :)");
 *     exampleTree.getNode(0).getNode(1).insert("C++ is used to make games.");
 *
 *     exampleTree.deepSearchChildren("BrainF***").get()
 *             .insert("Only uses eight pointers");
 *
 *     programmingLanguages.print();
 * </pre></p>
 * <p>This code would output the following:</p>
 * <pre>
 *     Languages
 *     ├── Compiled Languages
 *     │    ├── Java
 *     │    │    ├── Java is cool
 *     │    │    └── Android :)
 *     │    ├── C++
 *     │    │    └── C++ is used to make games.
 *     │    └── Go
 *     ├── Interpreted Languages
 *     │    ├── Python
 *     │    └── JavaScript
 *     └── Esoteric Languages
 *          └── BrainF***
 *  	         └── Only uses eight pointers
 * </pre>
 *
 * <p>Most of the calculations and method calls operate with a general <tt>O(n)</tt>
 * computational complexity. This is because every node is only visited once,
 * across every method provided in this class.</p>
 *
 * <p>Note: one should take care when providing a mutable object as the key value
 * initializing a node, for the behavior of the tree is not specified should
 * a node's {@link #equals(Object)} method be set to return a different value
 * in a separate thread.</p>
 *
 * @param <T> The type of the data stored in the tree.
 * @author github@mrodz
 * @since 8
 */
@SuppressWarnings("unused")
public class Tree<T> implements Collection<Tree<T>> {
    /**
     * The actual value stored in this node of the tree.
     */
    private T DATA;

    /**
     * The parent node to this instance. If this is the root node, its parent is {@code null}
     */
    private Tree<T> PARENT;

    /**
     * An {@link ArrayList} containing all nodes that are children to this node.
     */
    private final List<Tree<T>> CHILDREN = new ArrayList<>();

    /**
     * Used when printing the grid; ignore.
     */
    private boolean completed = false;

    /**
     * Stores all of the values associated with the children to this node.
     */
    private final Set<T> entries = new HashSet<>();

    /**
     * These are the characters used to visualize the tree.
     * <p>values: ['│', '└', '├', '─']</p>
     */
    private static final char[] SPECIAL_CHARACTERS = {'│', '└', '├', '─'};

    //
    // Preferences
    //

    /**
     * Specify whether or any special characters should be escaped when
     * getting a fancy {@link String} version of the table (preferred: {@code true}).
     * @see #cancelEscapeSequences
     */
    @Deprecated
    private boolean escapeCharacters = true;

    /**
     * Whether or not to use Java's native tab (\t) for horizontal spacing.
     */
    private static final boolean USE_NATIVE_TAB = false;

    /**
     * If {@link #USE_NATIVE_TAB} is {@code false}, how many spaces to use instead.
     */
    private static final int REPLACEMENT_TAB_SPACES = 3;

    //
    // CONSTRUCTORS
    //

    /**
     * Construct a new {@link Tree} with no starting node.
     *
     * @deprecated - a tree should contain a root element, for best clarity.
     */
    @Deprecated
    public Tree() {
    }

    /**
     * Construct a new {@link Tree} with a specific starting node.
     *
     * @param startingNode the node to serve as this tree's root.
     */
    public Tree(T startingNode) {
        if (startingNode == null) throw new NullPointerException("Root Node cannot be null");
        this.DATA = startingNode;
    }

    //
    // SPECIALIZED METHODS
    //

    /**
     * Add constructed nodes to this node (varargs).
     * This is the way in which all nodes are set to branch from
     * a parent node.
     *
     * @param nodes the nodes to be added.
     */
    @SafeVarargs
    public final void insert(Tree<T>... nodes) {
        if (Arrays.stream(nodes).anyMatch(Objects::isNull)) {
            throw new NullPointerException("null input");
        }
        for (Tree<T> node : nodes) {
            if (this.entries.contains(node.DATA)) {
                throw new IllegalArgumentException("Duplicate entry into tree: " + node);
            } else {
                node.setParent(this);
                this.addChild(node);
                this.entries.add(node.DATA);
            }
        }
    }

    /**
     * Create nodes to be added to this node from the raw input type (varargs).
     *
     * @param nodes the raw data to be added to this node.
     */
    @SafeVarargs
    public final void insert(T... nodes) {
        if (Arrays.stream(nodes).anyMatch(Objects::isNull)) {
            throw new NullPointerException("null input");
        }

        @SuppressWarnings("unchecked")
        Tree<T>[] nodes1 = new Tree[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            nodes1[i] = new Tree<>(nodes[i]);
        }

        insert(nodes1);
    }

    /**
     * Exclusively search the children that only span immediately from this node
     * (ie. depth of one) for a node with a matching value.
     *
     * @param data The object to be matched
     * @return An {@link Optional} containing the node with the matching {@link #DATA}
     * to the object supplied, if found; otherwise, {@link Optional#empty()}
     */
    public final Optional<Tree<T>> searchChildrenFor(final T data) {
        if (!this.entries.contains(data)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.CHILDREN.stream()
                .filter(n -> n.DATA.equals(data))
                .collect(Collectors.toList()).get(0));
    }

    /**
     * Search <u>every</u> child connected to this node for a node with a matching value.
     * This method will return the shallowest match exclusively, to avoid mix-ups.
     *
     * @param data The object to be matched
     * @return An {@link Optional} containing the node with the matching {@link #DATA}
     * to the object supplied, if found; otherwise, {@link Optional#empty()}
     * @see Tree#find(Object, Tree)
     */
    public final Optional<Tree<T>> deepSearchChildrenFor(final T data) {
        return Tree.find(data, this);
    }

    /**
     * Recursive function to search for an object in all of the nodes (children)
     * spanning from a specified target node. This method will return the shallowest match
     * exclusively, to avoid mix-ups.
     *
     * @param object The object
     * @param node   The parent node
     * @param <R>    The type of the returning {@link Optional}, the object being
     *               searched for, and the type of the {@link #DATA} being stored
     *               in the parent node.
     * @return An {@link Optional} containing the node with the matching {@link #DATA}
     * to the object supplied, if found; otherwise, {@link Optional#empty()}
     * @see #searchChildrenFor(Object)
     */
    public static <R> Optional<Tree<R>> find(final R object, Tree<R> node) {
        if (node.DATA.equals(object)) {
            return Optional.of(node);
        } else if (node.entries.contains(object)) {
            Stream<Tree<R>> stream = node.CHILDREN.stream().filter(n -> n.DATA.equals(object));
            List<Tree<R>> collected = stream.collect(Collectors.toList());
            if (collected.size() != 1) {
                throw new IllegalStateException("Multiple children to " + node + "have the same canonical value.");
            }
            return Optional.of(collected.get(0));
        }

        for (Tree<R> child : node.CHILDREN) {
            Optional<Tree<R>> obj = find(object, child);
            if (obj.isPresent()) {
                return obj;
            }
        }

        // Cannot find value
        return Optional.empty();
    }

    /**
     * Simple recursive tree traversal algorithm.
     *
     * @param res    Recursive field: to use, initialize a new {@link StringBuilder}
     * @param init   This is the grid to be traversed.
     * @param offset Recursive field, represents the amount of tab/spaces: to use, set to {@code 0}.
     * @param renderEscapeCharacters whether or not to render Java's escape
     *                               characters [\r,\n,\b, etc.] (preferably set
     *                               to {@code false}, seeing as messing with these
     *                               values could mess up the {@link String}'s
     *                               style).
     * @return a {@link String} containing a formatted tree, one that resembles the output from
     * the {@code tree} command in the windows command line.
     */
    private String buildFormattedTree(final StringBuilder res, Tree<T> init, int offset,
                                      final boolean renderEscapeCharacters) {
        List<Tree<T>> children = init.getChildren();

        // No children exist; the node is the last node in this lineage
        if (children.size() == 0) {
            return "";
        }

        // Go through each child
        for (int i = 0; i < children.size(); i++) {
            Tree<T> node = children.get(i);
            Tree<T> temp = init;
            StringBuilder str = new StringBuilder();

            // Apply the cosmetic tabs and pipes
            for (int j = 0; j < offset; j++) {
                temp = temp.getParent();
                str.append(Tree.USE_NATIVE_TAB ? '\t' : repeatCharacters.apply(Tree.REPLACEMENT_TAB_SPACES, ' '));
                str.append(temp.completed ? ' ' : SPECIAL_CHARACTERS[0]);
            }

            boolean isLastElement = i == children.size() - 1;

            res.append(str.reverse());
            res.append(isLastElement ? SPECIAL_CHARACTERS[1] : SPECIAL_CHARACTERS[2]);
            res.append(repeatCharacters.apply(2, SPECIAL_CHARACTERS[3]));

            // is the node the last node to appear in the sequence (visually)
            if (isLastElement) {
                node.getParent().completed = true;
            }

            res.append(' ').append(!renderEscapeCharacters
                    ? cancelEscapeSequences.apply(node.getNodeValue().toString())
                    : node.getNodeValue()).append("\r\n");

            buildFormattedTree(res, node, offset + 1, renderEscapeCharacters);
        }

        // Reset the 'completed field.'
        for (Tree<T> child : children) {
            child.getParent().completed = false;
        }

        // clear excess newline, return value
        return res.substring(0, res.length() - 1);
    }

    /**
     * Print this tree's content in a natural, easy to follow manner.
     * @see #buildFormattedTree(StringBuilder, Tree, int, boolean)
     * @see #print()
     */
    public void print() {
        print(false);
    }

    /**
     * Print this tree's content in a natural, easy to follow manner.
     *
     * @param renderEscapeCharacters whether or not to render Java's escape
     *                               characters [\r,\n,\b, etc.] (preferably set
     *                               to {@code false}, seeing as messing with these
     *                               values could mess up the {@link String}'s
     *                               style).
     * @see #buildFormattedTree(StringBuilder, Tree, int, boolean)
     */
    public void print(boolean renderEscapeCharacters) {
        System.out.println(this.getFancyString(renderEscapeCharacters));
    }

    /**
     * Get this tree's content in a fancy format. Keep in mind that the
     * style of the return value depends on the viewport, since smaller
     * STD I/O's might not be able to a show the entirety of a long value
     * on a single line.
     *
     * @param renderEscapeCharacters whether or not to render Java's escape
     *                               characters [\r,\n,\b, etc.] (preferably set
     *                               to {@code false}, seeing as messing with these
     *                               values could mess up the {@link String}'s
     *                               style).
     * @return a large formatted {@link String}
     * @see #print()
     */
    private String getFancyString(boolean renderEscapeCharacters) {
        // Handle deprecated functionality: data = null
        String str = this.DATA == null ? super.toString() : this.DATA.toString();
        str += this.getChildren().size() == 0 ? "" : "\r\n"+(buildFormattedTree(new StringBuilder(), this, 0, renderEscapeCharacters));
        return str;
    }

    /**
     * Get this tree's content in a fancy format. Keep in mind that the
     * style of the return value depends on the viewport, since smaller
     * STD I/O's might not be able to a show the entirety of a long value
     * on a single line.
     *
     * @return a large formatted {@link String}
     * @see #print()
     */
    public String getFancyString() {
        return getFancyString(false);
    }



    /**
     * Set a node as this tree's child.
     *
     * @param child the node
     */
    private void addChild(Tree<T> child) {
        this.CHILDREN.add(child);
    }

    //
    // GETTERS + SETTERS
    //

    /**
     * Get the Nth child to this node.
     *
     * @param index an {@code int} index
     * @return the {@link Tree} node at the specified index.
     * @throws IndexOutOfBoundsException if the index supplied is greater than the total amount
     *                                   of child nodes connected to this node, or less than zero.
     */
    public Tree<T> getNode(int index) throws IndexOutOfBoundsException {
        return CHILDREN.get(index);
    }

    /**
     * Get the actual value stored in this node.
     *
     * @return the data
     */
    public T getNodeValue() {
        return DATA;
    }

    /**
     * Update the actual value stored in this node.
     *
     * @param data the data
     */
    public void setNodeValue(T data) {
        this.DATA = data;
    }

    /**
     * Get this node's parent.
     *
     * @return the parent.
     */
    public Tree<T> getParent() {
        return PARENT;
    }

    /**
     * Set this node's parent.
     *
     * @param parent the parent.
     */
    private void setParent(Tree<T> parent) {
        this.PARENT = parent;
    }

    /**
     * Get the children to this node as a {@link List}.
     *
     * @return the children
     */
    public List<Tree<T>> getChildren() {
        return CHILDREN;
    }

    /**
     * Get the canonical values associated with all of the children to this node,
     * of type {@code T}
     *
     * @return {@link #entries}
     */
    public Set<T> getEntries() {
        return entries;
    }

    /**
     * Get whether this instance of {@link Tree} is escaping character
     * sequences in pretty Strings.
     *
     * @return whether it is or is not
     */
    @Deprecated
    public boolean isEscapingCharacters() {
        return escapeCharacters;
    }

    /**
     * Set whether this instance of {@link Tree} is escaping character
     * sequences in pretty Strings.
     *
     * @param escapeCharacters the value
     */
    @Deprecated
    public void setEscapingCharacters(boolean escapeCharacters) {
        this.escapeCharacters = escapeCharacters;
    }

    //
    // RESOURCES
    //

    /**
     * Function that returns a {@link String} repeating a character (c) a certain amount of times (reps).
     */
    private static final BiFunction<Integer, Character, String> repeatCharacters = (reps, c) ->
            String.valueOf(c).repeat(Math.max(0, reps));

    /**
     * Low-level function that returns a {@link String} canceling most ['\n', '\t', '\r']
     * of Java's escape characters.
     */
    public static final UnaryOperator<String> cancelEscapeSequences = (str) -> {
        StringBuilder res = new StringBuilder();
        char[] chars = str.toCharArray();
        for (char c : chars) {
            if (c == 8) res.append("\\b");
            else if (c == 9) res.append("\\t");
            else if (c == 10) res.append("\\n");
            else if (c == 13) res.append("\\r");
            else res.append(c);
        }
        return res.toString();
    };

    //
    // OVERRIDES
    //

    /**
     * Returns the number of elements in this collection.  If this collection
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of elements in this collection
     */
    @Override
    public int size() {
        return size(1, this);
    }

    /**
     * Used in {@link #size()} to get the count of nodes in this tree.
     * @param res recursive parameter; set to {@code 0}.
     * @param init the {@link Tree} to start at.
     * @return the count of nodes in this tree.
     */
    private int size(int res, Tree<T> init) {
        List<Tree<T>> children = init.getChildren();

        // No children exist; the node is the last node in this lineage
        if (children.size() == 0) {
            return res;
        }

        // Go through each child
        for (int i = 0; i < children.size(); i++, res++) {
            Tree<T> node = children.get(i);

            res += size(0, node);
        }

        return res;
    }

    /**
     * Returns {@code true} if this node has no children.
     *
     * @return {@code true} if this node has no children.
     */
    @Override
    public boolean isEmpty() {
        return this.CHILDREN.isEmpty();
    }

    /**
     * Returns {@code true} if this collection contains the specified element.
     * More formally, returns {@code true} if and only if this collection
     * contains at least one element {@code e} such that
     * {@code Objects.equals(o, e)}.
     *
     * <p>Equivalent to:</p>
     * <pre>
     * {@link #deepSearchChildrenFor(Object)}.{@link Optional#isPresent() isPresent()}
     * </pre>
     *
     * @param o element whose presence in this collection is to be tested
     * @return {@code true} if this collection contains the specified
     * element
     * @throws ClassCastException   if the type of the specified element
     *                              is incompatible with this collection
     * @throws NullPointerException if the specified element is null
     * @see #deepSearchChildrenFor(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) throws ClassCastException, NullPointerException {
        Objects.requireNonNull(o);
        return this.deepSearchChildrenFor((T) o).isPresent();
    }

    /**
     * Returns an iterator over this node's children. There are no
     * guarantees concerning the order in which the elements are returned
     * (unless this collection is an instance of some class that provides a
     * guarantee).
     *
     * @return an {@code Iterator} over the elements in this node's children.
     */
    @Override
    public Iterator<Tree<T>> iterator() {
        return this.CHILDREN.iterator();
    }

    /**
     * Returns an array containing all of this node's children.
     * If this collection makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements in
     * the same order. The returned array's {@linkplain Class#getComponentType
     * runtime component type} is {@code Object}.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this collection.  (In other words, this method must
     * allocate a new array even if this collection is backed by an array).
     * The caller is thus free to modify the returned array.
     *
     * @return an array, whose {@linkplain Class#getComponentType runtime component
     * type} is {@code Object}, containing all of the elements in this collection
     * @apiNote This method acts as a bridge between array-based and collection-based APIs.
     * It returns an array whose runtime type is {@code Object[]}.
     * Use {@link #toArray(Object[]) toArray(T[])} to reuse an existing
     * array.
     */
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(this.CHILDREN.toArray(), this.CHILDREN.size());
    }

    /**
     * Returns an array containing all of this node's children;
     * the runtime type of the returned array is that of the specified array.
     * If the collection fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this collection.
     *
     * <p>If this collection fits in the specified array with room to spare
     * (i.e., the array has more elements than this collection), the element
     * in the array immediately following the end of the collection is set to
     * {@code null}.  (This is useful in determining the length of this
     * collection <i>only</i> if the caller knows that this collection does
     * not contain any {@code null} elements.)
     *
     * <p>If this collection makes any guarantees as to what order its elements
     * are returned by its iterator, this method must return the elements in
     * the same order.
     *
     * @param a the array into which the elements of this collection are to be
     *          stored, if it is big enough; otherwise, a new array of the same
     *          runtime type is allocated for this purpose.
     * @return an array containing all of the elements in this collection
     * @throws ArrayStoreException  if the runtime type of any element in this
     *                              collection is not assignable to the {@linkplain Class#getComponentType
     *                              runtime component type} of the specified array
     * @throws NullPointerException if the specified array is null
     * @apiNote This method acts as a bridge between array-based and collection-based APIs.
     * It allows an existing array to be reused under certain circumstances.
     * Use {@link #toArray()} to create an array whose runtime type is {@code Object[]}.
     *
     * <p>Suppose {@code x} is a collection known to contain only strings.
     * The following code can be used to dump the collection into a previously
     * allocated {@code String} array:
     *
     * <pre>
     *     String[] y = new String[SIZE];
     *     ...
     *     y = x.toArray(y);</pre>
     *
     * <p>The return value is reassigned to the variable {@code y}, because a
     * new array will be allocated and returned if the collection {@code x} has
     * too many elements to fit into the existing array {@code y}.
     *
     * <p>Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <K> K[] toArray(K[] a) {
        return (K[]) this.CHILDREN.toArray(cast(a, Tree[].class));
    }

    /**
     * Ensures that this collection contains the specified element (optional
     * operation).  Returns {@code true} if this collection changed as a
     * result of the call.  (Returns {@code false} if this collection does
     * not permit duplicates and already contains the specified element.)<p>
     * <p>
     * Collections that support this operation may place limitations on what
     * elements may be added to this collection.  In particular, some
     * collections will refuse to add {@code null} elements, and others will
     * impose restrictions on the type of elements that may be added.
     * Collection classes should clearly specify in their documentation any
     * restrictions on what elements may be added.<p>
     * <p>
     * If a collection refuses to add a particular element for any reason
     * other than that it already contains the element, it <i>must</i> throw
     * an exception (rather than returning {@code false}).  This preserves
     * the invariant that a collection always contains the specified element
     * after this call returns.
     *
     * @param tTree element whose presence in this collection is to be ensured
     * @return {@code true} if this collection changed as a result of the
     * call
     * @throws ClassCastException            if the class of the specified element
     *                                       prevents it from being added to this collection
     * @throws NullPointerException          if the specified element is null and this
     *                                       collection does not permit null elements
     * @throws IllegalArgumentException      if some property of the element
     *                                       prevents it from being added to this collection
     * @throws IllegalStateException         if the element cannot be added at this
     *                                       time due to insertion restrictions
     */
    @Override
    public boolean add(Tree<T> tTree) {
        try {
            this.insert(tTree);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present (optional operation).  More formally,
     * removes an element {@code e} such that
     * {@code Objects.equals(o, e)}, if
     * this collection contains one or more such elements.  Returns
     * {@code true} if this collection contained the specified element (or
     * equivalently, if this collection changed as a result of the call).
     *
     * @param o element to be removed from this collection, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException            if the type of the specified element
     *                                       is incompatible with this collection
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if the specified element is null and this
     *                                       collection does not permit null elements
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    @Override
    public boolean remove(Object o) {
        return this.CHILDREN.remove(o);
    }

    /**
     * Returns {@code true} if this collection contains all of the elements
     * in the specified collection.
     *
     * @param c collection to be checked for containment in this collection
     * @return {@code true} if this collection contains all of the elements
     * in the specified collection
     * @throws ClassCastException   if the types of one or more elements
     *                              in the specified collection are incompatible with this
     *                              collection
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified collection contains one
     *                              or more null elements and this collection does not permit null
     *                              elements
     *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *                              or if the specified collection is null.
     * @see #contains(Object)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return this.CHILDREN.containsAll(c);
    }

    /**
     * Adds all of the elements in the specified collection to this collection
     * (optional operation).  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in progress.
     * (This implies that the behavior of this call is undefined if the
     * specified collection is this collection, and this collection is
     * nonempty.)
     *
     * @param c collection containing elements to be added to this collection
     * @return {@code true} if this collection changed as a result of the call
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this collection
     * @throws NullPointerException          if the specified collection contains a
     *                                       null element and this collection does not permit null elements,
     *                                       or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this
     *                                       collection
     * @throws IllegalStateException         if not all the elements can be added at
     *                                       this time due to insertion restrictions
     */
    @Override
    public boolean addAll(Collection<? extends Tree<T>> c) {
        try {
            @SuppressWarnings("unchecked")
            Tree<T>[] arr = (Tree<T>[]) Array.newInstance(Tree.class, 0);
            this.insert(c.toArray(arr));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes all of this collection's elements that are also contained in the
     * specified collection (optional operation).  After this call returns,
     * this collection will contain no elements in common with the specified
     * collection.
     *
     * @param c collection containing elements to be removed from this collection
     * @return {@code true} if this collection changed as a result of the
     * call
     * @throws ClassCastException            if the types of one or more elements
     *                                       in this collection are incompatible with the specified
     *                                       collection
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this collection contains one or more
     *                                       null elements and the specified collection does not support
     *                                       null elements
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        try {
            this.CHILDREN.removeAll(c);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retains only the elements in this collection that are contained in the
     * specified collection (optional operation).  In other words, removes from
     * this collection all of its elements that are not contained in the
     * specified collection.
     *
     * @param c collection containing elements to be retained in this collection
     * @return {@code true} if this collection changed as a result of the call
     * @throws ClassCastException            if the types of one or more elements
     *                                       in this collection are incompatible with the specified
     *                                       collection
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException          if this collection contains one or more
     *                                       null elements and the specified collection does not permit null
     *                                       elements
     *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
     *                                       or if the specified collection is null
     * @see #remove(Object)
     * @see #contains(Object)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        try {
            this.CHILDREN.retainAll(c);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recursive function to loop through every node in a tree.
     * @param init the node to start at.
     * @param action a {@link Consumer} action that accepts the
     * {@link #DATA} of the node.
     */
    private void forEach0(Tree<T> init, final Consumer<? super Tree<T>> action) {
        List<Tree<T>> children = init.getChildren();

        // No children exist; the node is the last node in this lineage
        if (children.size() == 0) {
            return;
        }

        // Go through each child
        for (Tree<T> node : children) {
            action.accept(node);
            forEach0(node, action);
        }
    }

    @Override
    public void forEach(Consumer<? super Tree<T>> action) {
        forEach0(this, action);
    }

    /**
     * Removes all of the elements from this collection (optional operation).
     * The collection will be empty after this method returns.
     */
    @Override
    public void clear() {
        this.CHILDREN.clear();
    }

    /**
     * Standard method
     * @param o another object
     * @return whether or not another object is equal to this instance.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tree<?> that = (Tree<?>) o;
        return Objects.equals(DATA, that.DATA) && Objects.equals(PARENT, that.PARENT) && Objects.equals(CHILDREN, that.CHILDREN);
    }

    /**
     * Get this object's hash code.
     *
     * @return Get this object's hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(DATA, CHILDREN);
    }

    /**
     * Provides a compacted representation of this table. For a prettier visualization,
     * see {@link #print()}
     *
     * @return a {@link String} object containing this node's value and its children.
     */
    @Override
    public String toString() {
        return String.format("%s[val: '%s' -> children: %s]", this.getClass().getSimpleName(), DATA, CHILDREN);
    }
}
