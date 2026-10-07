package edu.liceo.ugoautomate.ui.common;

import com.google.zxing.common.BitMatrix;
import edu.liceo.ugoautomate.ds.BTreeModel;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class DsDiagram extends Region {
    private enum Mode { EMPTY, BIT_GRID, CELLS, HASH, STACK, QUEUE, LINKED, BTREE }

    private static final Color CELL = Color.web("#FFF7E0");
    private static final Color CELL_BORDER = Color.web("#B08A2E");
    private static final Color EMPTY_CELL = Color.web("#EEEEEE");
    private static final Color NODE = Color.web("#E6EEFA");
    private static final Color NODE_BORDER = Color.web("#4A74C9");
    private static final Color ACCENT = Color.web("#7A1F2B");
    private static final Color MUTED = Color.web("#5F5A57");
    private static final Color LIGHT = Color.web("#C0C0C0");
    private static final double PAD = 12;
    private static final double KEY_LINE = 17;
    private static final double LEVEL_GAP = 40;

    private final Canvas canvas = new Canvas();
    private final Font font = Font.font("System", 12);
    private final Text measure = new Text();

    private Mode mode = Mode.EMPTY;
    private String emptyMessage = "";
    private BitMatrix matrix;
    private List<String> items = List.of();
    private int capacity;
    private BTreeModel tree;

    public DsDiagram() {
        getChildren().add(canvas);
        measure.setFont(font);
        getStyleClass().add("diagram-frame");
    }

    public void showEmpty(String message) {
        emptyMessage = message;
        set(Mode.EMPTY);
    }

    public void showBitGrid(BitMatrix m) {
        matrix = m;
        set(Mode.BIT_GRID);
    }

    public void showCells(List<String> values, int arrayCapacity) {
        items = List.copyOf(values);
        capacity = Math.max(arrayCapacity, values.size());
        set(Mode.CELLS);
    }

    public void showHashTable(List<String> keys, int bucketCount) {
        items = List.copyOf(keys);
        capacity = bucketCount;
        set(Mode.HASH);
    }

    public void showStack(List<String> topFirst) {
        items = List.copyOf(topFirst);
        set(Mode.STACK);
    }

    public void showQueue(List<String> oldestFirst) {
        items = List.copyOf(oldestFirst);
        set(Mode.QUEUE);
    }

    public void showLinkedList(List<String> inOrder) {
        items = List.copyOf(inOrder);
        set(Mode.LINKED);
    }

    public void showBTree(BTreeModel model) {
        tree = model;
        set(Mode.BTREE);
    }

    private void set(Mode newMode) {
        mode = newMode;
        requestLayout();
        draw();
    }

    public static int bucketOf(String key, int bucketCount) {
        int h = key.hashCode();
        return (h ^ (h >>> 16)) & (bucketCount - 1);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double height) {
        return mode == Mode.BTREE ? requiredWidth() : 200;
    }

    @Override
    protected double computePrefWidth(double height) {
        return Math.max(700, requiredWidth());
    }

    @Override
    protected double computePrefHeight(double width) {
        return heightFor(width > 0 ? width : 700);
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private double requiredWidth() {
        if (mode != Mode.BTREE || tree == null) {
            return 280;
        }
        double widest = 0;
        for (List<BTreeModel.Node> level : tree.levels()) {
            double w = 0;
            for (BTreeModel.Node node : level) {
                w += nodeWidth(node) + 14;
            }
            widest = Math.max(widest, w);
        }
        return widest + 2 * PAD;
    }

    private double heightFor(double width) {
        double inner = Math.max(200, width - 2 * PAD);
        return switch (mode) {
            case EMPTY -> 60;
            case BIT_GRID -> matrix == null ? 60 : bitCell(inner) * matrix.getHeight() + 50;
            case CELLS -> rows(capacity, perRow(inner, 120)) * 58 + 40;
            case HASH -> {
                int cols = Math.max(1, (int) (inner / 380));
                yield (capacity + cols - 1) / cols * 26 + 40;
            }
            case STACK -> Math.max(1, items.size()) * 38 + 60;
            case QUEUE -> rows(Math.max(1, items.size()), perRow(inner, 200)) * 58 + 50;
            case LINKED -> rows(Math.max(1, items.size()), perRow(inner, 200)) * 66 + 40;
            case BTREE -> tree == null ? 60 : bTreeHeight();
        };
    }

    private int bitCell(double inner) {
        return (int) Math.min(8, Math.max(3, (inner - 20) / matrix.getWidth()));
    }

    private static int perRow(double width, double cellWidth) {
        return Math.max(1, (int) (width / cellWidth));
    }

    private static int rows(int count, int perRow) {
        return Math.max(1, (count + perRow - 1) / perRow);
    }

    private void draw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setFont(font);
        g.setLineWidth(1);
        double inner = width - 2 * PAD;
        switch (mode) {
            case EMPTY -> caption(g, emptyMessage, PAD, 34);
            case BIT_GRID -> paintBitGrid(g, inner);
            case CELLS -> paintCells(g, inner);
            case HASH -> paintHash(g, inner);
            case STACK -> paintStack(g, inner);
            case QUEUE -> paintQueue(g, inner);
            case LINKED -> paintLinked(g, inner);
            case BTREE -> paintBTree(g, width);
        }
    }

    private void paintBitGrid(GraphicsContext g, double inner) {
        int n = matrix.getWidth();
        int cell = bitCell(inner);
        double x0 = PAD;
        double y0 = PAD + 18;
        caption(g, "Each square is one bit; every 32 squares in a row share one int (red lines).", PAD, PAD + 8);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < n; x++) {
                g.setFill(matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                g.fillRect(x0 + x * cell, y0 + y * cell, cell, cell);
            }
        }
        g.setStroke(Color.web("#DDDDDD"));
        g.strokeRect(x0, y0, n * cell, matrix.getHeight() * cell);
        g.setStroke(Color.rgb(220, 40, 40, 0.7));
        g.setLineWidth(2);
        for (int boundary = 32; boundary < n; boundary += 32) {
            g.strokeLine(x0 + boundary * cell, y0 - 4, x0 + boundary * cell, y0 + matrix.getHeight() * cell + 4);
        }
        g.setLineWidth(1);
    }

    private void paintCells(GraphicsContext g, double inner) {
        double cellW = 120;
        int perRow = perRow(inner, cellW);
        caption(g, "Contiguous Object[] - index i is at base + 16 + 4 × i bytes. Grey = spare capacity (null).", PAD, PAD + 8);
        for (int i = 0; i < capacity; i++) {
            double x = PAD + (i % perRow) * cellW;
            double y = PAD + 22 + (i / perRow) * 58;
            boolean used = i < items.size();
            box(g, x, y + 14, cellW, 34, used ? items.get(i) : "null", used ? CELL : EMPTY_CELL,
                    used ? CELL_BORDER : LIGHT, used ? Color.BLACK : Color.GRAY);
            g.setFill(MUTED);
            g.fillText("[" + i + "]", x + 4, y + 10);
        }
    }

    private void paintHash(GraphicsContext g, double inner) {
        List<List<String>> buckets = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            buckets.add(new ArrayList<>());
        }
        for (String key : items) {
            buckets.get(bucketOf(key, capacity)).add(key);
        }
        int cols = Math.max(1, (int) (inner / 380));
        double colWidth = inner / cols;
        int perCol = (capacity + cols - 1) / cols;
        caption(g, "bucket = (hash ^ hash >>> 16) & " + (capacity - 1) + "   - collisions are chained with next pointers", PAD, PAD + 8);
        for (int i = 0; i < capacity; i++) {
            double x = PAD + (i / perCol) * colWidth;
            double y = PAD + 20 + (i % perCol) * 26;
            List<String> chain = buckets.get(i);
            box(g, x, y, 40, 22, String.valueOf(i), chain.isEmpty() ? EMPTY_CELL : CELL,
                    chain.isEmpty() ? LIGHT : CELL_BORDER, Color.BLACK);
            double nodeW = chain.isEmpty() ? 0 : Math.min(150, (colWidth - 70) / chain.size() - 16);
            double nx = x + 58;
            for (String key : chain) {
                arrow(g, nx - 16, y + 11, nx, y + 11);
                box(g, nx, y, Math.max(40, nodeW), 22, key, NODE, NODE_BORDER, Color.BLACK);
                nx += Math.max(40, nodeW) + 16;
            }
        }
    }

    private void paintStack(GraphicsContext g, double inner) {
        if (items.isEmpty()) {
            caption(g, "The stack is empty.", PAD, PAD + 20);
            return;
        }
        double w = Math.min(320, inner - 120);
        double x = PAD + 90;
        caption(g, "push/pop happen only at the top (O(1)). Press Back to pop.", PAD, PAD + 8);
        for (int i = 0; i < items.size(); i++) {
            double y = PAD + 22 + i * 38;
            box(g, x, y, w, 32, items.get(i), i == 0 ? CELL : NODE, i == 0 ? CELL_BORDER : NODE_BORDER, Color.BLACK);
            if (i == 0) {
                g.setFill(ACCENT);
                g.fillText("top →", PAD + 30, y + 21);
            }
        }
        g.setFill(MUTED);
        g.fillText("bottom (first page opened)", x, PAD + 22 + items.size() * 38 + 14);
    }

    private void paintQueue(GraphicsContext g, double inner) {
        if (items.isEmpty()) {
            caption(g, "The queue is empty. Scan a QR code on the scanner page to add results.", PAD, PAD + 20);
            return;
        }
        double cellW = 200;
        int perRow = perRow(inner, cellW);
        caption(g, "poll() removes the oldest at the HEAD; offer() adds the newest at the TAIL (both O(1)).", PAD, PAD + 8);
        for (int i = 0; i < items.size(); i++) {
            double x = PAD + (i % perRow) * cellW;
            double y = PAD + 24 + (i / perRow) * 58;
            box(g, x, y + 14, cellW - 16, 32, items.get(i), NODE, NODE_BORDER, Color.BLACK);
            g.setFill(ACCENT);
            if (i == 0) {
                g.fillText("HEAD (oldest)", x, y + 10);
            }
            if (i == items.size() - 1) {
                g.fillText("TAIL (newest)", x + (i == 0 ? 100 : 0), y + 10);
            }
            if (i % perRow != perRow - 1 && i < items.size() - 1) {
                arrow(g, x + cellW - 16, y + 30, x + cellW, y + 30);
            }
        }
    }

    private void paintLinked(GraphicsContext g, double inner) {
        if (items.isEmpty()) {
            caption(g, "The list is empty.", PAD, PAD + 20);
            return;
        }
        double cellW = 200;
        int perRow = perRow(inner, cellW);
        caption(g, "Each node: [prev | value | next]. Nodes are scattered in memory; pointers keep the order.", PAD, PAD + 8);
        for (int i = 0; i < items.size(); i++) {
            double x = PAD + (i % perRow) * cellW;
            double y = PAD + 26 + (i / perRow) * 66;
            double w = cellW - 24;
            box(g, x, y + 14, w, 34, items.get(i), NODE, NODE_BORDER, Color.BLACK);
            g.setStroke(NODE_BORDER);
            g.strokeLine(x + 16, y + 14, x + 16, y + 48);
            g.strokeLine(x + w - 16, y + 14, x + w - 16, y + 48);
            g.setFill(ACCENT);
            if (i == 0) {
                g.fillText("head", x, y + 10);
            }
            if (i == items.size() - 1) {
                g.fillText("tail", x + w - 26, y + 10);
            }
            if (i % perRow != perRow - 1 && i < items.size() - 1) {
                arrow(g, x + w, y + 25, x + cellW, y + 25);
                arrow(g, x + cellW, y + 37, x + w, y + 37);
            }
        }
    }

    private double nodeHeight(BTreeModel.Node node) {
        return 8 + KEY_LINE * Math.max(1, node.keys().size());
    }

    private double levelHeight(List<BTreeModel.Node> level) {
        return level.stream().mapToDouble(this::nodeHeight).max().orElse(30);
    }

    private double bTreeHeight() {
        double height = 2 * PAD + 20;
        for (List<BTreeModel.Node> level : tree.levels()) {
            height += levelHeight(level) + LEVEL_GAP;
        }
        return height;
    }

    private record Box(double x, double y, double w, double h) {
    }

    private void paintBTree(GraphicsContext g, double canvasWidth) {
        List<List<BTreeModel.Node>> levels = tree.levels();
        List<List<Box>> boxes = new ArrayList<>();
        double width = Math.max(canvasWidth, requiredWidth());
        double y = PAD + 20;
        for (List<BTreeModel.Node> level : levels) {
            double total = 0;
            for (BTreeModel.Node node : level) {
                total += nodeWidth(node) + 14;
            }
            double x = Math.max(PAD, (width - total) / 2);
            List<Box> row = new ArrayList<>();
            for (BTreeModel.Node node : level) {
                double w = nodeWidth(node);
                row.add(new Box(x, y, w, nodeHeight(node)));
                x += w + 14;
            }
            boxes.add(row);
            y += levelHeight(level) + LEVEL_GAP;
        }

        g.setStroke(Color.GRAY);
        for (int li = 0; li < levels.size() - 1; li++) {
            int child = 0;
            for (int p = 0; p < levels.get(li).size(); p++) {
                Box parent = boxes.get(li).get(p);
                int count = levels.get(li).get(p).childCount();
                for (int c = 0; c < count && child < boxes.get(li + 1).size(); c++, child++) {
                    Box target = boxes.get(li + 1).get(child);
                    double px = parent.x() + (c + 0.5) * parent.w() / count;
                    g.strokeLine(px, parent.y() + parent.h(), target.x() + target.w() / 2, target.y());
                }
            }
        }
        for (int li = 0; li < levels.size(); li++) {
            boolean leaf = li == levels.size() - 1;
            Color border = leaf ? CELL_BORDER : NODE_BORDER;
            for (int n = 0; n < levels.get(li).size(); n++) {
                Box r = boxes.get(li).get(n);
                List<String> keys = levels.get(li).get(n).keys();
                box(g, r.x(), r.y(), r.w(), r.h(), "", leaf ? CELL : NODE, border, Color.BLACK);
                g.setTextBaseline(VPos.CENTER);
                for (int k = 0; k < keys.size(); k++) {
                    double top = r.y() + 4 + KEY_LINE * k;
                    g.setFill(Color.BLACK);
                    g.fillText(shortKey(keys.get(k)), r.x() + 8, top + KEY_LINE / 2);
                    if (k > 0) {
                        g.setStroke(border);
                        g.strokeLine(r.x() + 4, top, r.x() + r.w() - 4, top);
                    }
                }
                g.setTextBaseline(VPos.BASELINE);
            }
        }
        g.setFill(MUTED);
        g.fillText("root (interior keys separate the children)", PAD, PAD + 8);
        g.fillText("leaves: all keys in sorted order", PAD, y - LEVEL_GAP + 18);
    }

    private double nodeWidth(BTreeModel.Node node) {
        double widest = node.keys().stream().mapToDouble(k -> textWidth(shortKey(k))).max().orElse(24);
        return Math.max(40, widest + 16);
    }

    private static String shortKey(String key) {
        return key.length() <= 16 ? key : key.substring(0, 15) + "…";
    }

    private double textWidth(String text) {
        measure.setText(text);
        return measure.getLayoutBounds().getWidth();
    }

    private void box(GraphicsContext g, double x, double y, double w, double h, String text,
                     Color fill, Color border, Color fg) {
        g.setFill(fill);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setStroke(border);
        g.strokeRoundRect(x, y, w, h, 8, 8);
        if (text.isEmpty()) {
            return;
        }
        g.setFill(fg);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(fit(text, w - 8), x + w / 2, y + h / 2);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.BASELINE);
    }

    private String fit(String text, double width) {
        if (textWidth(text) <= width) {
            return text;
        }
        String s = text;
        while (s.length() > 1 && textWidth(s + "…") > width) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    private static void arrow(GraphicsContext g, double x1, double y1, double x2, double y2) {
        g.setStroke(Color.DARKGRAY);
        g.strokeLine(x1, y1, x2, y2);
        double dir = x2 >= x1 ? -1 : 1;
        g.strokeLine(x2, y2, x2 + dir * 5, y2 - 4);
        g.strokeLine(x2, y2, x2 + dir * 5, y2 + 4);
    }

    private static void caption(GraphicsContext g, String text, double x, double y) {
        g.setFill(MUTED);
        g.fillText(text, x, y);
    }
}
