package edu.liceo.ugoautomate.ds;

import java.util.List;

public final class StructureCatalog {
    private static final List<StructureInfo> ALL = List.of(
            new StructureInfo(StructureKind.ARRAY, "Array (2D bit grid)",
                    "int[] inside ZXing BitMatrix",
                    "Every QR code (attendance QR, entry QR, guest pass) is built as a fixed grid of black/white "
                            + "modules. ZXing packs 32 modules into each int of one flat int[] array "
                            + "(util/QrCodes.java). Module (x, y) is found at bits[y * rowSize + x / 32].",
                    "One contiguous block allocated once on the heap; its size is fixed when created. "
                            + "Because elements sit side by side, the address of element i is "
                            + "base + i × elementSize, which is why access by index is O(1).",
                    "O(n)",
                    List.of(
                            new Complexity("Access by index", "O(1)", "O(1)", "O(1)"),
                            new Complexity("Search (unsorted, linear)", "O(1)", "O(n)", "O(n)"),
                            new Complexity("Search (sorted, binary search)", "O(1)", "O(log n)", "O(log n)"),
                            new Complexity("Insert / delete (shift elements)", "O(1)", "O(n)", "O(n)"))),

            new StructureInfo(StructureKind.DYNAMIC_ARRAY, "Dynamic Array (ArrayList)",
                    "java.util.ArrayList<T>",
                    "Every list you see in a table (accounts, attendance records, sessions, entry logs, locations) "
                            + "is loaded from the database into an ArrayList (dao/impl/AbstractJdbcDao.queryList) "
                            + "and displayed from an ArrayList (ui/common/ListTableModel). Clicking a column "
                            + "header sorts it with TimSort.",
                    "A contiguous Object[] of 4-byte references with spare capacity. When it is full, "
                            + "a new array 1.5× larger is allocated and all references are copied over, so adding "
                            + "at the end is O(1) amortized. The element objects themselves live elsewhere on the heap.",
                    "O(n)",
                    List.of(
                            new Complexity("get(i) - access by index", "O(1)", "O(1)", "O(1)"),
                            new Complexity("add(x) - append at end", "O(1)", "O(1) amortized", "O(n) when resizing"),
                            new Complexity("add(i, x) / remove(i)", "O(1)", "O(n)", "O(n)"),
                            new Complexity("contains(x) - linear search", "O(1)", "O(n)", "O(n)"),
                            new Complexity("sort (column click, TimSort)", "O(n)", "O(n log n)", "O(n log n)"))),

            new StructureInfo(StructureKind.HASH_TABLE, "Hash Table (HashMap)",
                    "java.util.HashMap<String, Long>",
                    "The QR scanner remembers recently processed codes in a HashMap (code → time seen) so the "
                            + "same QR held in front of the webcam is recorded only once (ui/common/QrScanPanel). "
                            + "The diagram hashes the real campus location names into buckets exactly the way "
                            + "Java's HashMap does: index = (h ^ (h >>> 16)) & (capacity - 1).",
                    "A bucket array whose length is a power of two, plus one separately allocated Node "
                            + "(32 bytes) per entry. Keys that land in the same bucket are chained in a linked list "
                            + "(turned into a red-black tree after 8 collisions). When size exceeds 0.75 × capacity "
                            + "the bucket array doubles and every entry is rehashed.",
                    "O(n)",
                    List.of(
                            new Complexity("get / containsKey", "O(1)", "O(1)", "O(log n) *"),
                            new Complexity("put", "O(1)", "O(1)", "O(n) when resizing"),
                            new Complexity("remove", "O(1)", "O(1)", "O(log n) *"),
                            new Complexity("iterate all entries", "O(capacity + n)", "O(capacity + n)", "O(capacity + n)"))),

            new StructureInfo(StructureKind.STACK, "Stack (LIFO)",
                    "java.util.ArrayDeque<String> used as a stack",
                    "The Back button in the dashboard header. Every page you open is pushed onto a stack; "
                            + "Back pops the top page and returns you to the previous one "
                            + "(ui/common/DashboardController). The diagram shows your live navigation history.",
                    "ArrayDeque stores elements in a circular array. push/pop only move the head index, so "
                            + "no node is allocated per element. When the array is full it grows "
                            + "(doubles while small, then +50%) and elements are copied.",
                    "O(n)",
                    List.of(
                            new Complexity("push (open a page)", "O(1)", "O(1) amortized", "O(n) when resizing"),
                            new Complexity("pop (Back)", "O(1)", "O(1)", "O(1)"),
                            new Complexity("peek (current page)", "O(1)", "O(1)", "O(1)"),
                            new Complexity("search", "O(1)", "O(n)", "O(n)"))),

            new StructureInfo(StructureKind.QUEUE, "Queue (FIFO, bounded)",
                    "java.util.ArrayDeque<String> used as a queue",
                    "The \"Recent scans\" list on the Scan Attendance and Entry Gate pages. New results are added "
                            + "at the tail; when more than 12 are kept, the oldest is removed from the head "
                            + "(ui/common/ScanHistory). The diagram shows your live scan history.",
                    "The same circular array as the stack: head and tail indexes wrap around the end of the "
                            + "array, so removing the oldest item never shifts the others.",
                    "O(n)",
                    List.of(
                            new Complexity("offer (add newest at tail)", "O(1)", "O(1) amortized", "O(n) when resizing"),
                            new Complexity("poll (remove oldest at head)", "O(1)", "O(1)", "O(1)"),
                            new Complexity("peek", "O(1)", "O(1)", "O(1)"),
                            new Complexity("iterate", "O(n)", "O(n)", "O(n)"))),

            new StructureInfo(StructureKind.DOUBLY_LINKED_LIST, "Doubly Linked List (LinkedHashMap)",
                    "java.util.LinkedHashMap<String, View>",
                    "The dashboard keeps its pages in a LinkedHashMap (ui/common/DashboardController). The hash "
                            + "table finds a page by name in O(1); a doubly linked list running through the entries "
                            + "remembers insertion order, which is the order of the menu buttons.",
                    "Each entry is its own node object (40 bytes) holding hash, key, value, next (bucket chain), "
                            + "and before/after pointers (the doubly linked list). Nodes are scattered anywhere on "
                            + "the heap - order comes from the pointers, not from positions in memory.",
                    "O(n)",
                    List.of(
                            new Complexity("find page by name (hash)", "O(1)", "O(1)", "O(log n)"),
                            new Complexity("insert at tail", "O(1)", "O(1)", "O(1)"),
                            new Complexity("remove a node (relink)", "O(1)", "O(1)", "O(1)"),
                            new Complexity("iterate in order (build menu)", "O(n)", "O(n)", "O(n)"),
                            new Complexity("access the k-th element", "O(1)", "O(n)", "O(n)"))),

            new StructureInfo(StructureKind.B_TREE, "B-Tree (database index)",
                    "SQLite B-tree index (CREATE INDEX ...)",
                    "Every index in db/schema.sql (users.email, locations.name, attendance by student, entry logs "
                            + "by time, ...) is a B-tree. It is why login finds your account and the logs page finds "
                            + "a day's entries without scanning the whole table. The diagram builds a small B-tree "
                            + "(order 4) from the real location names.",
                    "Stored in fixed-size 4 KB pages on disk. Each page holds many sorted keys, so even a million "
                            + "rows need only about 3-4 levels. SQLite keeps recently used pages cached in memory.",
                    "O(n)",
                    List.of(
                            new Complexity("search (e.g. login by email)", "O(log n)", "O(log n)", "O(log n)"),
                            new Complexity("insert", "O(log n)", "O(log n)", "O(log n)"),
                            new Complexity("delete", "O(log n)", "O(log n)", "O(log n)"),
                            new Complexity("range scan (k results)", "O(log n + k)", "O(log n + k)", "O(log n + k)")))
    );

    private StructureCatalog() {
    }

    public static List<StructureInfo> all() {
        return ALL;
    }
}
