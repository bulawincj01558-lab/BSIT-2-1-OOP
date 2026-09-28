package edu.liceo.ugoautomate.ui.common;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A sortable, filterable, single-selection table with a search box and a row
 * count, used by every list screen.
 */
public class DataTable<T> {

    private static final int MIN_COLUMN_WIDTH = 120;

    private final ListTableModel<T> model;
    private final JTable table;
    private final TableRowSorter<ListTableModel<T>> sorter;
    private final JTextField filterField = new JTextField(16);
    private final JLabel countLabel = new JLabel();
    private final JPanel component = new JPanel(new BorderLayout(0, 8));

    public DataTable(ListTableModel<T> model) {
        this.model = model;
        this.table = new JTable(model);
        this.sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        filterField.putClientProperty("JTextField.placeholderText", "Type to filter...");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        JPanel top = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 2));
        top.setOpaque(false);
        top.add(new JLabel("Search:"));
        top.add(filterField);
        countLabel.setForeground(UiTheme.TEXT_MUTED);
        top.add(countLabel);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(300, 320));
        scrollPane.setMinimumSize(new Dimension(160, 160));
        component.setOpaque(false);
        component.add(top, BorderLayout.NORTH);
        component.add(scrollPane, BorderLayout.CENTER);
        updateCount();
        Responsive.onResize(scrollPane, () -> fitColumns(scrollPane.getViewport().getWidth()));
    }

    /**
     * Columns stretch to fill wide screens; on narrow screens each column keeps
     * a readable width and the table scrolls horizontally inside its own box,
     * so the page layout itself never breaks.
     */
    private void fitColumns(int viewportWidth) {
        int columns = table.getColumnCount();
        if (viewportWidth <= 0 || columns == 0) {
            return;
        }
        boolean narrow = viewportWidth < columns * MIN_COLUMN_WIDTH;
        int mode = narrow ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
        if (table.getAutoResizeMode() != mode) {
            table.setAutoResizeMode(mode);
            if (narrow) {
                for (int i = 0; i < columns; i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(MIN_COLUMN_WIDTH);
                }
            }
        }
    }

    public JComponent getComponent() {
        return component;
    }

    public JTable getTable() {
        return table;
    }

    public void setRows(List<T> rows) {
        model.setRows(rows);
        updateCount();
    }

    public Optional<T> getSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return Optional.empty();
        }
        return Optional.of(model.getRow(table.convertRowIndexToModel(viewRow)));
    }

    /** Runs {@code action} when a row is double-clicked. */
    public void onDoubleClick(Runnable action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    action.run();
                }
            }
        });
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        updateCount();
    }

    private void updateCount() {
        int shown = table.getRowCount();
        int total = model.getRowCount();
        countLabel.setText(shown == total ? total + " record(s)" : shown + " of " + total + " record(s)");
    }
}
