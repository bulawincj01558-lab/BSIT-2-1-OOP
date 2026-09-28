package edu.liceo.ugoautomate.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Data Structures reference page (linked to the Data Structures subject).
 * Describes each linear structure the application actually uses: where it is
 * used, how its memory is allocated, and the time complexity of its operations.
 * <p>
 * Memory figures assume a 64-bit HotSpot JVM with compressed references (the
 * default for heaps under 32 GB): 12-byte object header, 4-byte references,
 * objects padded to a multiple of 8 bytes.
 */
public class DataStructuresPanel extends JPanel {

    /** Everything shown for one data structure. */
    private record Info(String title, String javaType, String summary, DsDiagram.Kind diagram,
                        String usedIn, String memory, String[][] operations) {
    }

    private static final String[] OPERATION_COLUMNS = {"Operation", "Time", "Why"};
    private static final int MIN_COLUMN_WIDTH = 110;

    private static final Info[] STRUCTURES = {
            new Info("1. Array", "String[], byte[]  (Java built-in)",
                    "A fixed-length sequence of elements stored side by side in one block of memory. "
                            + "The address of element i is computed directly: base + i x elementSize.",
                    DsDiagram.Kind.ARRAY,
                    "- RegisterStudentDialog.COURSES: the 16 course choices (String[16]).\n"
                            + "- QR signing key: 32 random bytes (byte[32]) in SecretKeyProvider / QrTokenService.\n"
                            + "- QrTokenService.verify: a scanned QR payload is split into its 6 parts (String[6]).",
                    "Allocated once, all at the same time, with a length that never changes.\n"
                            + "Size = 16-byte header (12 header + 4 length) + n x element size, rounded up to 8.\n"
                            + "- byte[32] QR key = 16 + 32 = 48 bytes\n"
                            + "- String[6] payload parts = 16 + 6 x 4 = 40 bytes (references only; each String is its own object)\n"
                            + "- String[16] courses = 16 + 16 x 4 = 80 bytes",
                    new String[][]{
                            {"Access by index  a[i]", "O(1)", "Address is computed directly"},
                            {"Update by index  a[i] = x", "O(1)", "Same direct address"},
                            {"Search (unsorted)", "O(n)", "May have to check every element"},
                            {"Insert / delete", "O(n)", "Length is fixed: a new array must be allocated and copied"},
                            {"Space", "O(n)", "Exactly n slots"},
                    }),
            new Info("2. Dynamic Array", "java.util.ArrayList",
                    "An array that resizes itself. It keeps spare capacity at the end; when full, it allocates "
                            + "a bigger array (1.5x) and copies the elements over.",
                    DsDiagram.Kind.DYNAMIC_ARRAY,
                    "- AbstractJdbcDao.query: every database result (students, sessions, records, logs) is collected "
                            + "into an ArrayList.\n"
                            + "- ListTableModel: the rows of every table on screen.",
                    "ArrayList object = 24 bytes (header 12 + size 4 + modCount 4 + array reference 4),\n"
                            + "plus the backing Object[] = 16 + 4 x capacity bytes.\n"
                            + "Capacity starts at 10 on the first add, then grows 10 -> 15 -> 22 -> 33 -> 49 -> ... "
                            + "Each growth allocates a new array and the old one becomes garbage.\n"
                            + "Example: 500 attendance records -> capacity 549 -> 16 + 549 x 4 = 2,212 bytes of references "
                            + "(up to ~50% spare slots).",
                    new String[][]{
                            {"get(i) / set(i, x)", "O(1)", "Backed by an array"},
                            {"add(x) at the end", "O(1) amortized", "O(n) only on the rare resize + copy"},
                            {"add(i, x) / remove(i)", "O(n)", "Elements after i are shifted"},
                            {"contains / indexOf", "O(n)", "Linear scan"},
                            {"Space", "O(n)", "n references + spare capacity"},
                    }),
            new Info("3. Linked List", "java.util.LinkedList (doubly linked)",
                    "A chain of nodes; each node stores the element plus references to the previous and next "
                            + "node. Nodes are allocated one at a time and can live anywhere in memory.",
                    DsDiagram.Kind.LINKED_LIST,
                    "- QrScanPanel \"Recent scans\": the last 10 scan results, newest first. Each new result is "
                            + "added with addFirst() and the oldest is dropped with removeLast().",
                    "LinkedList object = 32 bytes (header 12 + size 4 + modCount 4 + first 4 + last 4 = 28, padded to 32).\n"
                            + "Each Node = 24 bytes (header 12 + item 4 + next 4 + prev 4), allocated on every add "
                            + "and freed on remove. No resizing or copying ever happens.\n"
                            + "Example: 10 recent scans = 32 + 10 x 24 = 272 bytes (plus the Strings themselves). "
                            + "About 6x more overhead per element than an array.",
                    new String[][]{
                            {"addFirst / addLast", "O(1)", "Only the head or tail links change"},
                            {"removeFirst / removeLast", "O(1)", "Only the head or tail links change"},
                            {"get(i)", "O(n)", "Walks from the nearer end (up to n/2 steps)"},
                            {"contains", "O(n)", "Linear scan"},
                            {"Insert/remove at a known node", "O(1)", "Re-link neighbours, no shifting"},
                    }),
            new Info("4. Stack (LIFO)", "java.util.ArrayDeque used with push / pop / peek",
                    "Last In, First Out: the most recently added item is the first removed. Java recommends "
                            + "ArrayDeque over the legacy Stack class.",
                    DsDiagram.Kind.STACK,
                    "- DashboardFrame Back button: every time you open a page the previous page is pushed; "
                            + "Back pops it. History is capped at 30 pages.",
                    "ArrayDeque object = 24 bytes (header 12 + array reference 4 + head 4 + tail 4).\n"
                            + "Backing circular Object[] starts with 17 slots = 16 + 17 x 4 = 84 -> 88 bytes. "
                            + "When full it roughly doubles (while small), then grows by 50%.\n"
                            + "push/pop only move the head index, so elements are never shifted.",
                    new String[][]{
                            {"push(x)", "O(1) amortized", "Write at head; O(n) only on resize"},
                            {"pop()", "O(1)", "Read and clear the head slot"},
                            {"peek()", "O(1)", "Read the head slot"},
                            {"isEmpty / size", "O(1)", "Computed from head and tail"},
                            {"search", "O(n)", "Linear scan"},
                    }),
            new Info("5. Queue (FIFO)", "java.util.ArrayDeque used with offer / poll / peek",
                    "First In, First Out: items leave in the same order they arrived, like a line at the gate.",
                    DsDiagram.Kind.QUEUE,
                    "- QrScanPanel pending scans: if a code is scanned while another is still being verified "
                            + "(e.g. several students at the gate), it waits in the queue and is verified next.",
                    "Same ArrayDeque layout as the stack: 24-byte object + circular Object[] (17 slots initially).\n"
                            + "offer() writes at the tail, poll() reads at the head; both indices wrap around the "
                            + "array, so dequeuing never shifts the remaining elements (unlike removing index 0 of an ArrayList, "
                            + "which is O(n)).",
                    new String[][]{
                            {"offer(x) - enqueue", "O(1) amortized", "Write at tail; O(n) only on resize"},
                            {"poll() - dequeue", "O(1)", "Read the head, advance head index"},
                            {"peek()", "O(1)", "Read the head slot"},
                            {"isEmpty / size", "O(1)", "Computed from head and tail"},
                            {"search", "O(n)", "Linear scan"},
                    }),
    };

    public DataStructuresPanel() {
        super(new BorderLayout());
        JPanel cards = new JPanel(new ResponsiveGridLayout(520, 2, 16, 16));
        cards.setOpaque(false);
        cards.add(summaryCard());
        for (Info info : STRUCTURES) {
            cards.add(structureCard(info));
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(cards, BorderLayout.NORTH);
        add(UiTheme.page("Data Structures",
                "Linear data structures used inside Liceo U Go Automate, with their memory allocation and time complexity.",
                holder), BorderLayout.CENTER);
    }

    private static JPanel summaryCard() {
        String[] columns = {"Structure", "Access", "Search", "Insert", "Delete", "Memory / element"};
        String[][] rows = {
                {"Array", "O(1)", "O(n)", "O(n)", "O(n)", "4 B ref (exact)"},
                {"ArrayList", "O(1)", "O(n)", "O(1)* end", "O(n)", "4 B ref + spare"},
                {"LinkedList", "O(n)", "O(n)", "O(1) ends", "O(1) ends", "24 B node"},
                {"Stack", "O(1) top", "O(n)", "O(1)* push", "O(1) pop", "4 B ref + spare"},
                {"Queue", "O(1) front", "O(n)", "O(1)* offer", "O(1) poll", "4 B ref + spare"},
        };
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(table(columns, rows), BorderLayout.NORTH);
        body.add(UiTheme.paragraph("* amortized: usually O(1), occasionally O(n) when the backing array is resized.\n"
                + "Memory figures assume a 64-bit JVM with compressed references (12-byte object header, "
                + "4-byte references, 8-byte alignment)."), BorderLayout.CENTER);
        return card("Summary: time complexity and memory", body);
    }

    private static JPanel structureCard(Info info) {
        JLabel type = new JLabel(info.javaType());
        type.setForeground(UiTheme.TEXT_MUTED);
        type.setFont(type.getFont().deriveFont(Font.ITALIC));

        JPanel body = new FormBuilder()
                .addFull(type)
                .addFull(UiTheme.paragraph(info.summary()))
                .addFull(new DsDiagram(info.diagram()))
                .addFull(heading("Where it is used in this app"))
                .addFull(UiTheme.paragraph(info.usedIn()))
                .addFull(heading("Memory allocation"))
                .addFull(UiTheme.paragraph(info.memory()))
                .addFull(heading("Time complexity"))
                .addFull(table(OPERATION_COLUMNS, info.operations()))
                .build();
        return card(info.title(), body);
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        label.setForeground(UiTheme.PRIMARY_DARK);
        return label;
    }

    /**
     * A read-only table shown at full height. On narrow screens the columns
     * keep a readable width and the table scrolls sideways inside its own box.
     */
    private static JScrollPane table(String[] columns, String[][] rows) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE2DADC)));
        int height = header.getPreferredSize().height + table.getRowHeight() * rows.length + 18;
        scroll.setPreferredSize(new Dimension(300, height));
        scroll.setMinimumSize(new Dimension(120, height));
        int minTableWidth = columns.length * MIN_COLUMN_WIDTH;
        Responsive.onResize(scroll, () -> {
            int width = scroll.getViewport().getWidth();
            if (width <= 0) {
                return;
            }
            boolean narrow = width < minTableWidth;
            table.setAutoResizeMode(narrow ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            if (narrow) {
                for (int i = 0; i < columns.length; i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(MIN_COLUMN_WIDTH);
                }
            }
        });
        return scroll;
    }

    private static JPanel card(String title, JPanel body) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2DADC)), UiTheme.padding(14)));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        label.setForeground(UiTheme.PRIMARY);
        body.setOpaque(false);
        card.add(label, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }
}
