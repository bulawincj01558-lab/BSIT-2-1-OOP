package edu.liceo.ugoautomate.ui.common;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Read-only table model backed by a list of domain objects. Each column is a
 * header plus a function extracting the cell value from a row object.
 */
public class ListTableModel<T> extends AbstractTableModel {

    /** A table column definition. */
    public record Column<R>(String name, Class<?> type, Function<R, Object> value) {
    }

    private final List<Column<T>> columns;
    /** Table rows in a dynamic array: getRow(i) is O(1), which JTable calls for every visible cell. */
    private List<T> rows = new ArrayList<>();

    @SafeVarargs
    public ListTableModel(Column<T>... columns) {
        this.columns = List.of(columns);
    }

    public static <R> Column<R> column(String name, Function<R, Object> value) {
        return new Column<>(name, Object.class, value);
    }

    public static <R> Column<R> column(String name, Class<?> type, Function<R, Object> value) {
        return new Column<>(name, type, value);
    }

    public void setRows(List<T> newRows) {
        this.rows = new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    public T getRow(int modelIndex) {
        return rows.get(modelIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).name();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns.get(column).type();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return columns.get(column).value().apply(rows.get(row));
    }
}
