package edu.liceo.ugoautomate.ui.common;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Small memory-layout diagram for a linear data structure. Cells shrink to
 * fit the available width so diagrams stay readable on narrow screens.
 */
public class DsDiagram extends JComponent {

    /** Which structure to draw. */
    public enum Kind { ARRAY, DYNAMIC_ARRAY, LINKED_LIST, STACK, QUEUE }

    private static final Color CELL = new Color(0xF1E1E4);
    private static final Color CELL_BORDER = new Color(0x7A0019);
    private static final Color EMPTY_BORDER = new Color(0xB9AEB1);
    private static final Color TEXT = new Color(0x2A1418);
    private static final Color NOTE = new Color(0x5F5F66);

    private final Kind kind;

    public DsDiagram(Kind kind) {
        this.kind = kind;
        int height = switch (kind) {
            case STACK -> 176;
            case LINKED_LIST -> 118;
            default -> 104;
        };
        setPreferredSize(new Dimension(360, height));
        setMinimumSize(new Dimension(200, height));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        switch (kind) {
            case ARRAY -> drawArray(g);
            case DYNAMIC_ARRAY -> drawDynamicArray(g);
            case LINKED_LIST -> drawLinkedList(g);
            case STACK -> drawStack(g);
            case QUEUE -> drawQueue(g);
        }
        g.dispose();
    }

    private void drawArray(Graphics2D g) {
        String[] values = {"BSIT", "BSCS", "BSIS", "BSN", "BSA", "BSBA"};
        int cell = cellWidth(values.length, 70);
        note(g, "One contiguous block, fixed length " + values.length + ", index -> address", 4, 14);
        for (int i = 0; i < values.length; i++) {
            int x = 4 + i * cell;
            box(g, x, 26, cell, 38, values[i], true);
            centered(g, "[" + i + "]", x, 80, cell, NOTE);
        }
    }

    private void drawDynamicArray(Graphics2D g) {
        int capacity = 10;
        int size = 6;
        int cell = cellWidth(capacity, 52);
        note(g, "size = " + size + ", capacity = " + capacity + "  (grows x1.5 when full)", 4, 14);
        for (int i = 0; i < capacity; i++) {
            int x = 4 + i * cell;
            box(g, x, 26, cell, 38, i < size ? "r" + (i + 1) : "", i < size);
            centered(g, String.valueOf(i), x, 80, cell, NOTE);
        }
    }

    private void drawLinkedList(Graphics2D g) {
        String[] items = {"scan 4", "scan 3", "scan 2", "scan 1"};
        int gap = 22;
        int node = Math.min(110, (getWidth() - 8 - gap * (items.length - 1)) / items.length);
        note(g, "Separate nodes anywhere on the heap, linked by prev/next references", 4, 14);
        int y = 40;
        int h = 40;
        for (int i = 0; i < items.length; i++) {
            int x = 4 + i * (node + gap);
            g.setColor(CELL);
            g.fillRoundRect(x, y, node, h, 8, 8);
            g.setColor(CELL_BORDER);
            g.setStroke(new BasicStroke(1.6f));
            g.drawRoundRect(x, y, node, h, 8, 8);
            int part = node / 5;
            g.drawLine(x + part, y, x + part, y + h);
            g.drawLine(x + node - part, y, x + node - part, y + h);
            centered(g, items[i], x + part, y + h / 2 + 5, node - 2 * part, TEXT);
            if (i < items.length - 1) {
                int x2 = x + node + gap;
                arrow(g, x + node - part / 2, y + 12, x2, y + 12);        // next
                arrow(g, x2 + part / 2, y + h - 12, x + node, y + h - 12); // prev
            }
        }
        centered(g, "head", 4, y - 6, node, NOTE);
        centered(g, "tail", 4 + (items.length - 1) * (node + gap), y - 6, node, NOTE);
        note(g, "[prev | item | next]  ->  addFirst / removeLast touch only one end", 4, y + h + 20);
    }

    private void drawStack(Graphics2D g) {
        String[] items = {"Accounts", "Attendance Sessions", "Entry Verification", "Home"};
        int w = Math.min(190, getWidth() - 150);
        int x = 8;
        int h = 30;
        note(g, "Back-button history (LIFO): last page visited is on top", 4, 14);
        for (int i = 0; i < items.length; i++) {
            box(g, x, 26 + i * h, w, h, items[i], true);
        }
        arrow(g, x + w + 60, 26 + h / 2, x + w + 6, 26 + h / 2);
        g.setColor(TEXT);
        g.drawString("top", x + w + 66, 26 + h / 2 + 4);
        g.setColor(NOTE);
        g.drawString("push() adds here", x + w + 12, 26 + h + 18);
        g.drawString("pop() removes here", x + w + 12, 26 + h + 34);
    }

    private void drawQueue(Graphics2D g) {
        String[] items = {"code A", "code B", "code C", "code D", ""};
        int cell = cellWidth(items.length, 80);
        note(g, "Pending scans (FIFO): first in is first verified", 4, 14);
        for (int i = 0; i < items.length; i++) {
            box(g, 4 + i * cell, 26, cell, 38, items[i], !items[i].isEmpty());
        }
        g.setColor(NOTE);
        g.drawString("front: poll()", 4, 84);
        String rear = "rear: offer()";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rear, 4 + items.length * cell - fm.stringWidth(rear), 84);
    }

    // ------------------------------------------------------------ helpers

    private int cellWidth(int cells, int max) {
        return Math.max(26, Math.min(max, (getWidth() - 8) / cells));
    }

    private void box(Graphics2D g, int x, int y, int w, int h, String text, boolean filled) {
        Stroke original = g.getStroke();
        if (filled) {
            g.setColor(CELL);
            g.fillRect(x, y, w, h);
            g.setColor(CELL_BORDER);
            g.setStroke(new BasicStroke(1.6f));
        } else {
            g.setColor(EMPTY_BORDER);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
        }
        g.drawRect(x, y, w, h);
        g.setStroke(original);
        centered(g, text, x, y + h / 2 + 5, w, TEXT);
    }

    private static void centered(Graphics2D g, String text, int x, int baseline, int width, Color color) {
        FontMetrics fm = g.getFontMetrics();
        String shown = text;
        while (fm.stringWidth(shown) > width - 4 && shown.length() > 1) {
            shown = shown.substring(0, shown.length() - 1);
        }
        g.setColor(color);
        g.drawString(shown, x + (width - fm.stringWidth(shown)) / 2, baseline);
    }

    private static void note(Graphics2D g, String text, int x, int baseline) {
        g.setColor(NOTE);
        g.drawString(text, x, baseline);
    }

    private static void arrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(CELL_BORDER);
        g.setStroke(new BasicStroke(1.6f));
        g.drawLine(x1, y1, x2, y2);
        int dir = x2 >= x1 ? -1 : 1;
        g.fillPolygon(new int[]{x2, x2 + dir * 7, x2 + dir * 7}, new int[]{y2, y2 - 4, y2 + 4}, 3);
    }
}
