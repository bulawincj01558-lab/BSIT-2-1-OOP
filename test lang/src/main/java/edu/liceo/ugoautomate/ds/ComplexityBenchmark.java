package edu.liceo.ugoautomate.ds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

public final class ComplexityBenchmark {
    public record Result(String operation, String structure, String bigO, long[] nanosPerOp) {
    }

    public static final int[] SIZES = {1_000, 10_000, 100_000};

    private static final long WARMUP_NANOS = 15_000_000L;
    private static final long MEASURE_NANOS = 25_000_000L;

    private static volatile long sink;

    private ComplexityBenchmark() {
    }

    private interface Case {
        String operation();

        String structure();

        String bigO();

        IntSupplier prepare(int n);
    }

    private record SimpleCase(String operation, String structure, String bigO,
                              java.util.function.IntFunction<IntSupplier> setup) implements Case {
        @Override
        public IntSupplier prepare(int n) {
            return setup.apply(n);
        }
    }

    private static List<Case> cases() {
        return List.of(
                new SimpleCase("Access middle element", "Array int[]", "O(1)", n -> {
                    int[] a = filledArray(n);
                    return () -> a[n / 2];
                }),
                new SimpleCase("Access middle element", "ArrayList.get", "O(1)", n -> {
                    List<Integer> list = new ArrayList<>(boxed(n));
                    return () -> list.get(n / 2);
                }),
                new SimpleCase("Access middle element", "LinkedList.get", "O(n)", n -> {
                    List<Integer> list = new LinkedList<>(boxed(n));
                    return () -> list.get(n / 2);
                }),
                new SimpleCase("Search missing value", "Array linear scan", "O(n)", n -> {
                    int[] a = filledArray(n);
                    return () -> {
                        for (int v : a) {
                            if (v == -1) {
                                return 1;
                            }
                        }
                        return 0;
                    };
                }),
                new SimpleCase("Search missing value", "ArrayList.contains", "O(n)", n -> {
                    List<Integer> list = new ArrayList<>(boxed(n));
                    Integer missing = -1;
                    return () -> list.contains(missing) ? 1 : 0;
                }),
                new SimpleCase("Search missing value", "Binary search (sorted array)", "O(log n)", n -> {
                    int[] a = filledArray(n);
                    return () -> Arrays.binarySearch(a, -1);
                }),
                new SimpleCase("Search missing value", "HashSet.contains (hash table)", "O(1)", n -> {
                    Set<Integer> set = new HashSet<>(boxed(n));
                    Integer missing = -1;
                    return () -> set.contains(missing) ? 1 : 0;
                }),
                new SimpleCase("Insert at front", "ArrayList.add(0, x)", "O(n)", n -> {
                    ArrayList<Integer> list = new ArrayList<>(boxed(n));
                    Integer x = 7;
                    return () -> {
                        list.add(0, x);
                        list.remove(0);
                        return list.size();
                    };
                }),
                new SimpleCase("Insert at front", "ArrayDeque.addFirst (stack push)", "O(1)", n -> {
                    ArrayDeque<Integer> deque = new ArrayDeque<>(boxed(n));
                    Integer x = 7;
                    return () -> {
                        deque.addFirst(x);
                        deque.pollFirst();
                        return deque.size();
                    };
                }),
                new SimpleCase("Insert at front", "LinkedList.addFirst", "O(1)", n -> {
                    LinkedList<Integer> list = new LinkedList<>(boxed(n));
                    Integer x = 7;
                    return () -> {
                        list.addFirst(x);
                        list.removeFirst();
                        return list.size();
                    };
                })
        );
    }

    public static List<Result> run() {
        List<Result> results = new ArrayList<>();
        for (Case c : cases()) {
            long[] nanos = new long[SIZES.length];
            for (int i = 0; i < SIZES.length; i++) {
                IntSupplier op = c.prepare(SIZES[i]);
                measure(op, WARMUP_NANOS);
                nanos[i] = measure(op, MEASURE_NANOS);
            }
            results.add(new Result(c.operation(), c.structure(), c.bigO(), nanos));
        }
        return results;
    }

    private static long measure(IntSupplier op, long budget) {
        long ops = 0;
        long acc = 0;
        long start = System.nanoTime();
        long elapsed;
        do {
            for (int i = 0; i < 16; i++) {
                acc += op.getAsInt();
            }
            ops += 16;
            elapsed = System.nanoTime() - start;
        } while (elapsed < budget);
        sink = acc;
        return Math.max(1, elapsed / ops);
    }

    private static int[] filledArray(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    private static List<Integer> boxed(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return list;
    }
}
