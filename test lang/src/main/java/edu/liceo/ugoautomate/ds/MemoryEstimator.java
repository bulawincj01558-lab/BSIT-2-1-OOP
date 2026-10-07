package edu.liceo.ugoautomate.ds;

import java.util.ArrayList;
import java.util.List;

public final class MemoryEstimator {
    public static final int OBJECT_HEADER = 12;
    public static final int ARRAY_HEADER = 16;
    public static final int REFERENCE = 4;
    public static final int INT = 4;
    public static final int SQLITE_PAGE = 4096;

    public record Line(String part, long bytes) {
    }

    public record Report(List<Line> lines, long totalBytes, String note) {
    }

    private MemoryEstimator() {
    }

    public static long align(long bytes) {
        return (bytes + 7) / 8 * 8;
    }

    public static long referenceArray(long length) {
        return align(ARRAY_HEADER + REFERENCE * length);
    }

    public static long intArray(long length) {
        return align(ARRAY_HEADER + INT * length);
    }

    public static int arrayListCapacity(int n) {
        if (n == 0) {
            return 0;
        }
        int capacity = 10;
        while (capacity < n) {
            capacity = capacity + (capacity >> 1);
        }
        return capacity;
    }

    public static int hashMapCapacity(int n) {
        int capacity = 16;
        while (n > capacity * 3 / 4) {
            capacity <<= 1;
        }
        return capacity;
    }

    public static int arrayDequeCapacity(int n) {
        int capacity = 17;
        while (n > capacity - 1) {
            capacity += capacity < 64 ? capacity + 2 : capacity >> 1;
        }
        return capacity;
    }

    public static Report bitMatrix(int width, int height) {
        int rowSize = (width + 31) / 32;
        long object = align(OBJECT_HEADER + 3 * INT + REFERENCE);
        long bits = intArray((long) rowSize * height);
        List<Line> lines = List.of(
                new Line("BitMatrix object (width, height, rowSize, bits ref)", object),
                new Line("int[] bits: " + rowSize + " ints/row × " + height + " rows = " + (rowSize * height) + " ints", bits));
        return new Report(lines, object + bits, width + " × " + height + " = " + (width * height)
                + " modules stored as bits, 32 per int, in ONE contiguous array.");
    }

    public static Report arrayList(int n) {
        int capacity = arrayListCapacity(n);
        long object = align(OBJECT_HEADER + 2 * INT + REFERENCE);
        long array = capacity == 0 ? 0 : referenceArray(capacity);
        List<Line> lines = List.of(
                new Line("ArrayList object (size, modCount, elementData ref)", object),
                new Line("Object[] elementData: capacity " + capacity + " × 4-byte refs", array),
                new Line("  of which unused spare capacity: " + (capacity - n) + " slots", (long) REFERENCE * (capacity - n)));
        return new Report(lines, object + array, n + " elements, capacity " + capacity
                + ". Unused slots are the price of O(1) amortized append.");
    }

    public static Report hashMap(int n) {
        int capacity = hashMapCapacity(n);
        long object = align(OBJECT_HEADER + 5 * INT + 3 * REFERENCE);
        long table = referenceArray(capacity);
        long node = align(OBJECT_HEADER + INT + 3 * REFERENCE);
        List<Line> lines = List.of(
                new Line("HashMap object (size, threshold, loadFactor, table ref, ...)", object),
                new Line("Node[] table: " + capacity + " buckets × 4-byte refs", table),
                new Line(n + " Node objects × " + node + " bytes (hash, key, value, next)", node * n));
        return new Report(lines, object + table + node * n, n + " entries in " + capacity
                + " buckets (load factor " + String.format("%.2f", n / (double) capacity) + "). Nodes are separate heap objects.");
    }

    public static Report arrayDeque(int n, String usage) {
        int capacity = arrayDequeCapacity(n);
        long object = align(OBJECT_HEADER + 2 * INT + REFERENCE);
        long array = referenceArray(capacity);
        List<Line> lines = List.of(
                new Line("ArrayDeque object (head, tail, elements ref)", object),
                new Line("Object[] circular array: " + capacity + " × 4-byte refs", array));
        return new Report(lines, object + array, n + " " + usage + " in a circular array of " + capacity
                + " slots. No per-element nodes are allocated.");
    }

    public static Report linkedHashMap(int n) {
        int capacity = hashMapCapacity(n);
        long object = align(OBJECT_HEADER + 5 * INT + 5 * REFERENCE + 1);
        long table = referenceArray(capacity);
        long entry = align(OBJECT_HEADER + INT + 5 * REFERENCE);
        List<Line> lines = List.of(
                new Line("LinkedHashMap object (+ head, tail pointers)", object),
                new Line("Entry[] table: " + capacity + " buckets", table),
                new Line(n + " Entry nodes × " + entry + " bytes (hash, key, value, next, before, after)", entry * n));
        return new Report(lines, object + table + entry * n, n + " nodes linked in insertion order by before/after pointers.");
    }

    public static Report bTreeIndex(int n, int averageKeySize) {
        int cellSize = averageKeySize + 12;
        int keysPerPage = Math.max(2, (int) (SQLITE_PAGE * 0.9) / cellSize);
        long leafPages = Math.max(1, (n + keysPerPage - 1) / keysPerPage);
        long pages = leafPages;
        long levelPages = leafPages;
        int height = 1;
        while (levelPages > 1) {
            levelPages = (levelPages + keysPerPage) / (keysPerPage + 1);
            pages += levelPages;
            height++;
        }
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("≈ " + keysPerPage + " keys fit in one 4 KB page (key ≈ " + averageKeySize + " B + row id)", 0));
        lines.add(new Line(leafPages + " leaf page(s)", leafPages * SQLITE_PAGE));
        lines.add(new Line((pages - leafPages) + " interior page(s)", (pages - leafPages) * SQLITE_PAGE));
        return new Report(lines, pages * SQLITE_PAGE, n + " keys, tree height " + height
                + ". Height grows as log base " + (keysPerPage + 1) + " of n, so 1,000,000 rows need only ~"
                + treeHeight(1_000_000, keysPerPage) + " page reads.");
    }

    static int treeHeight(long n, int keysPerPage) {
        long pages = Math.max(1, (n + keysPerPage - 1) / keysPerPage);
        int height = 1;
        while (pages > 1) {
            pages = (pages + keysPerPage) / (keysPerPage + 1);
            height++;
        }
        return height;
    }

    public static String format(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
