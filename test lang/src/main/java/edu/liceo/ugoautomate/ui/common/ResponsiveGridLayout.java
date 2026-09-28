package edu.liceo.ugoautomate.ui.common;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Grid whose column count follows the available width: as many columns of at
 * least {@code minCellWidth} as fit, up to {@code maxColumns}. Cards sit side
 * by side on desktops and landscape tablets and stack into one column on
 * narrow or portrait screens, without changing their order.
 */
public class ResponsiveGridLayout implements LayoutManager {

    private final int minCellWidth;
    private final int maxColumns;
    private final int hgap;
    private final int vgap;

    public ResponsiveGridLayout(int minCellWidth, int maxColumns, int hgap, int vgap) {
        this.minCellWidth = minCellWidth;
        this.maxColumns = Math.max(1, maxColumns);
        this.hgap = hgap;
        this.vgap = vgap;
    }

    int columnsFor(int width) {
        int columns = (width + hgap) / (minCellWidth + hgap);
        return Math.max(1, Math.min(maxColumns, columns));
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            List<Component> cells = visible(parent);
            int available = parent.getWidth() - insets.left - insets.right;
            int columns = parent.getWidth() > 0 ? columnsFor(available) : Math.min(maxColumns, Math.max(1, cells.size()));
            int cellWidth = minCellWidth;
            for (Component c : cells) {
                cellWidth = Math.max(cellWidth, c.getPreferredSize().width);
            }
            int width = columns * cellWidth + (columns - 1) * hgap;
            return new Dimension(width + insets.left + insets.right,
                    totalHeight(cells, columns) + insets.top + insets.bottom);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int width = minCellWidth;
            for (Component c : visible(parent)) {
                width = Math.min(Math.max(width, c.getMinimumSize().width), minCellWidth);
            }
            return new Dimension(width + insets.left + insets.right,
                    totalHeight(visible(parent), 1) + insets.top + insets.bottom);
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            List<Component> cells = visible(parent);
            int available = parent.getWidth() - insets.left - insets.right;
            int columns = columnsFor(available);
            int cellWidth = (available - (columns - 1) * hgap) / columns;
            // Give every cell its final width first, so wrapped text inside it
            // reports the correct preferred height for that width.
            for (Component c : cells) {
                if (c.getWidth() != cellWidth) {
                    c.setSize(cellWidth, Math.max(1, c.getHeight()));
                    c.validate();
                }
            }
            int y = insets.top;
            for (int rowStart = 0; rowStart < cells.size(); rowStart += columns) {
                int rowEnd = Math.min(rowStart + columns, cells.size());
                int rowHeight = 0;
                for (int i = rowStart; i < rowEnd; i++) {
                    rowHeight = Math.max(rowHeight, cells.get(i).getPreferredSize().height);
                }
                for (int i = rowStart; i < rowEnd; i++) {
                    int x = insets.left + (i - rowStart) * (cellWidth + hgap);
                    cells.get(i).setBounds(x, y, cellWidth, rowHeight);
                }
                y += rowHeight + vgap;
            }
            // If the heights changed because of the new width, ask the parent
            // to lay out again once so the page gets the correct scroll height.
            int needed = totalHeight(cells, columns) + insets.top + insets.bottom;
            if (needed != parent.getHeight() && needed != lastRequestedHeight) {
                lastRequestedHeight = needed;
                SwingUtilities.invokeLater(parent::revalidate);
            }
        }
    }

    private int lastRequestedHeight = -1;

    private int totalHeight(List<Component> cells, int columns) {
        int height = 0;
        for (int rowStart = 0; rowStart < cells.size(); rowStart += columns) {
            int rowHeight = 0;
            for (int i = rowStart; i < Math.min(rowStart + columns, cells.size()); i++) {
                rowHeight = Math.max(rowHeight, cells.get(i).getPreferredSize().height);
            }
            height += rowHeight + (rowStart > 0 ? vgap : 0);
        }
        return height;
    }

    private static List<Component> visible(Container parent) {
        List<Component> cells = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            if (c.isVisible()) {
                cells.add(c);
            }
        }
        return cells;
    }
}
