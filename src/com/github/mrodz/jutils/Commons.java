package com.github.mrodz.jutils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>Utility Class. Standard methods across my projects, meant to be statically
 * imported.</p>
 * <p>Usage:</p>
 * <pre>import static com.github.mrodz.jutils.Commons.*;</pre>
 */
public class Commons {
    private Commons() {
        throw new IllegalStateException("cannot be instantiated");
    }

    /**
     * Returns an {@code int[]} array that spans from the <tt>minIncl</tt> value
     * to the <tt>maxIncl</tt>.
     * <p>Example:</p>
     * <pre>
     *     int[] arr = range(-3, 7);
     *     System.out.println(Arrays.toString(arr));
     *     // [-3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7]
     * </pre>
     * @param minIncl the minimum bound to the range, inclusive.
     * @param maxIncl the maximum bound to the range, inclusive.
     * @return an {@code int[]} array that spans from the <tt>minIncl</tt> value
     *         to the <tt>maxIncl</tt>
     */
    public static int[] range(int minIncl, int maxIncl) {
        if (minIncl >= maxIncl) return new int[0];
        int[] result = new int[safeInt(Math.abs((long) maxIncl - minIncl) + 1)];
        for (int i = minIncl; i <= maxIncl; i++)
            result[i - minIncl] = i;
        return result;
    }

    /**
     * Safely cast from {@code long} to {@code int}.
     * @param val a {@code long} value.
     * @return {@link Integer#MIN_VALUE} or {@link Integer#MAX_VALUE} if <tt>val</tt>
     *         surpasses the bounds of a 32-bit number; else, <tt>val</tt>.
     */
    public static int safeInt(long val) {
        return val < Integer.MIN_VALUE ? Integer.MIN_VALUE : val > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) val;
    }

    /**
     * Implementation of a variable that is not statically typed.
     * Below are the ways implementations can use this variable.
     * <pre>
     *     $ anyVar;
     *
     *     anyVar = $(5L);
     *     long asInt = anyVar.it();
     *     System.out.println(anyVar.getType()); // java.lang.Long
     *
     *     anyVar = $("Hello, Java");
     *     String asString = anyVar.it();
     *     System.out.println(anyVar.getType()); // java.lang.String
     * </pre>
     * <p>Use the following methods to create these objects. </p>
     * @see #$()
     * @see #$(Object)
     */
    public static final class $ {
        /**
         * The current type of the dynamic variable.
         */
        private final Class<?> type;
        /**
         * The value of the dynamic variable, of class {@link #type}
         */
        private final Object value;

        // private constructor
        private <T> $(Object value, Class<T> type) {
            this.type = type;
            this.value = value;
        }

        /**
         * Get the value of the dynamic variable.
         * @param <T> the variable's type.
         * @return the value of the dynamic variable.
         * @throws ClassCastException if the variable and its usage are incompatible,
         * ie. if a dynamic variable holding a {@link String} were declared as
         * an {@code int} variable.
         */
        @SuppressWarnings("unchecked")
        public <T> T it() throws ClassCastException {
            return (T) cast(this.value, this.type);
        }

        /**
         * Return the current type of the dynamic variable.
         * @return {@link #type}
         */
        public Class<?> getType() {
            return this.type;
        }

        /**
         * Overriding {@link Object#toString()}
         * @return the {@link String} representation of {@link #value}
         */
        @Override
        public String toString() {
            return String.valueOf(this.value);
        }
    }

    /**
     * Create an empty (unset) dynamic variable.
     */
    public static $ $() {
        return new $(null, null);
    }

    /**
     * Create a dynamic variable.
     * <p>Usage:</p>
     * <pre>
     *     $ anyVar;
     *     anyVar = $("Hello, World!");
     *     anyVar = $(5.3F);
     *     anyVar = $(new JPanel(new BorderLayout()));
     * </pre>
     * @param val the value of the dynamic variable.
     * @param <T> its type.
     */
    public static <T> $ $(T val) {
        return new $(val, val.getClass());
    }

    /**
     * Stop the current execution of a program, wait <tt>millis</tt>
     * milliseconds, and then run <tt>action</tt>.
     * @param millis {@code long} value, represents the amount of milliseconds to pause for.
     * @param action the code to be run.
     * @throws InterruptedException if the waiting was interrupted.
     * @see #doAfter(long, Runnable)
     */
    public static void doAfter(long millis, Thread action) throws InterruptedException {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        this.wait(millis);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("InterruptedException");
                }
                action.run();
            }
        };

        t.setName("Thread-doAfter()");

        t.start();
        t.join();
    }

    /**
     * Stop the current execution of a program, wait <tt>millis</tt>
     * milliseconds, and then run <tt>action</tt>.
     * @param millis {@code long} value, represents the amount of milliseconds to pause for.
     * @param action the code to be run.
     * @throws InterruptedException if the waiting was interrupted.
     * @see #doAfter(long, Thread)
     */
    public static void doAfter(long millis, Runnable action) throws InterruptedException {
        doAfter(millis, new Thread(action));
    }

    /**
     * Wait <tt>millis</tt> milliseconds, and then run <tt>action</tt>. The code will
     * run synchronously in a separate thread.
     * @param millis {@code long} value, represents the amount of milliseconds to pause for.
     * @param action the code to be run.
     * @throws IllegalStateException if the waiting was interrupted.
     * @see #doAfterSynchronously(long, Runnable)
     */
    public static void doAfterSynchronously(long millis, Thread action) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        this.wait(millis);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("InterruptedException");
                }
                action.run();
            }
        };

        t.setName("Thread-doAfterAsync()");

        t.start();
    }

    /**
     * Wait <tt>millis</tt> milliseconds, and then run <tt>action</tt>. The code will
     * run synchronously in a separate thread.
     * @param millis {@code long} value, represents the amount of milliseconds to pause for.
     * @param action the code to be run.
     * @throws IllegalStateException if the waiting was interrupted.
     * @see #doAfterSynchronously(long, Thread)
     */
    public static void doAfterSynchronously(long millis, Runnable action) {
        doAfterSynchronously(millis, new Thread(action));
    }

    /**
     * Iterate over all of the {@link Thread Threads} supplied, calling
     * each of them successively (new threads are only started after
     * the previous ones complete).
     * @param actions a {@code varargs} list of threads to be ran.
     * @throws InterruptedException if this method is interrupted.
     */
    public static void doSuccessively(Thread... actions) throws InterruptedException {
        Thread t = new Thread() {
            @Override
            public void run() {
                for (Runnable r : actions) {
                    Thread individualRunnable = new Thread(r);
                    individualRunnable.start();

                    synchronized (this) {
                        try {
                            individualRunnable.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("InterruptedException");
                        }
                    }
                }
            }
        };
        t.start();
        t.join();
    }

    /**
     * Iterate over all of the {@link Runnable} objects supplied, calling
     * each of them successively (new threads are only started after
     * the previous ones complete).
     * @param actions a {@code varargs} list of runnable objects to be ran.
     * @throws InterruptedException if this method is interrupted.
     */
    public static void doSuccessively(Runnable... actions) throws InterruptedException {
        doSuccessively(Arrays.stream(actions).map(Thread::new).toArray(Thread[]::new));
    }

    /**
     * Iterate over all of the {@link Thread Threads} supplied, calling
     * each of them successively (new threads are only started after
     * the previous ones complete). This process is realized synchronously
     * in its own independent thread.
     *
     * @param actions a {@code varargs} list of threads to be ran.
     * @throws IllegalStateException if this method is interrupted.
     */
    public static void doSuccessivelySynchronously(Thread... actions) {
        Thread t = new Thread() {
            @Override
            public void run() {
                for (Runnable r : actions) {
                    Thread individualRunnable = new Thread(r);
                    individualRunnable.start();

                    synchronized (this) {
                        try {
                            individualRunnable.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("InterruptedException");
                        }
                    }
                }
            }
        };
        t.start();
    }

    /**
     * Iterate over all of the {@link Runnable} objects supplied, calling
     * each of them successively (new threads are only started after
     * the previous ones complete). This process is realized synchronously
     * in its own independent thread.
     *
     * @param actions a {@code varargs} list of runnable objects to be ran.
     * @throws IllegalStateException if this method is interrupted.
     */
    public static void doSuccessivelySynchronously(Runnable... actions) {
        doSuccessivelySynchronously(Arrays.stream(actions).map(Thread::new).toArray(Thread[]::new));
    }

    /**
     * Return the calling class's {@link Class} object.
     * @return the calling class's {@link Class} object.
     */
    public static Class<?> thisClass() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        try {
            return Class.forName(elements[2].getClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not get this class.");
        }
    }

    /**
     * Cast an object to another type. Also works with arrays of any type.
     * <p>Example:</p>
     * <pre>
     * Integer[][] matrice = {{1, 2, 3}, {4, 5, 6}};
     * int[][] matriceAsInt = cast(matrice, int[][].class);
     * System.out.println(Arrays.deepToString(matriceAsInt));
     * </pre>
     * Relies heavily on reflection, may throw corresponding Exceptions.
     *
     * @param original the {@link Object} to be casted.
     * @param castTo   namesake; a variable of type {@link Class}.
     * @param <T>      the type to which <tt>original</tt> will be cast.
     * @return (( castTo) original)
     * @throws ClassCastException if <tt>original</tt> cannot be cast to <tt>castTo</tt>
     * @see Array
     * @see Class#cast(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object original, Class<T> castTo) throws ClassCastException {
        if (castTo.isArray() && original.getClass().isArray()) {
            T array = (T) Array.newInstance(castTo.getComponentType(), Array.getLength(original));
            for (int i = 0; i < Array.getLength(original); i++)
                Array.set(array, i, cast(Array.get(original, i), castTo.getComponentType()));
            return array;
        } else {
            return (T) original;
        }
    }

    /**
     * Return true only if all of the conditions are true.
     * @param booleans a list of boolean values.
     * @return true only if all of the conditions are true.
     */
    public static boolean all(boolean... booleans) {
        for (boolean b : booleans) {
            if (!b) return false;
        }
        return true;
    }

    /**
     * Return true if any of the conditions are true.
     * @param booleans a list of boolean values.
     * @return true if any of the conditions are true.
     */
    public static boolean any(boolean... booleans) {
        for (boolean b : booleans) {
            if (b) return true;
        }
        return false;
    }

    /**
     * Returns true if the {@link Class} object supplied is a wrapper
     * for a primitive type.
     * @param clazz a {@link Class} object.
     * @return true if the {@link Class} object supplied is a wrapper
     *         for a primitive type.
     * @see <a href="https://docs.oracle.com/javase/9/docs/api/java/lang/package-summary.html">
     *     Primitive Wrappers</a>
     */
    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        return clazz.equals(Boolean.class) || clazz.equals(Byte.class) || clazz.equals(Short.class)
                || clazz.equals(Integer.class) || clazz.equals(Long.class) || clazz.equals(Float.class)
                || clazz.equals(Double.class) || clazz.equals(Character.class);
    }

    /**
     * Iterate over every element in an array.
     * @param array an {@link Object}, presumably an array, of any type.
     * @param action a {@link Consumer} that is applied to every element in the array.
     * @param <T> the type of the array elements.
     * @throws IllegalArgumentException if <tt>array</tt> is not actually an array.
     * @see Array Array Reflection in Java
     */
    @SuppressWarnings("unchecked")
    public static <T> void forEach(Object array, Consumer<T> action) throws IllegalArgumentException {
        Objects.requireNonNull(array);
        Objects.requireNonNull(action);

        if (!array.getClass().isArray()) throw new IllegalArgumentException("array is not an array");

        for (int i = 0; i < Array.getLength(array); i++) {
            action.accept((T) Array.get(array, i));
        }
    }

    /**
     * Short for {@link Math#max(int, int)}
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * Short for {@link Math#max(long, long)}
     */
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /**
     * Short for {@link Math#max(float, float)}
     */
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * Short for {@link Math#max(double, double)}
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * Short for {@link Math#min(int, int)}
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    /**
     * Short for {@link Math#min(long, long)}
     */
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    /**
     * Short for {@link Math#min(float, float)}
     */
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /**
     * Short for {@link Math#min(double, double)}
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }
}
