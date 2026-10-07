package edu.liceo.ugoautomate.ui.common;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Tables {
    private static final double ROW_HEIGHT = 32;
    private static final double HEADER_HEIGHT = 36;

    private Tables() {
    }

    public static <T> void setup(TableView<T> table, String emptyMessage) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(emptyMessage));
        table.setFixedCellSize(ROW_HEIGHT);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> TableColumn<T, Object> column(TableView<T> table, String title, Function<? super T, ?> getter) {
        TableColumn<T, Object> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        column.setComparator(Comparator.nullsFirst((a, b) -> a instanceof Comparable ca && a.getClass() == b.getClass()
                ? ca.compareTo(b) : String.valueOf(a).compareToIgnoreCase(String.valueOf(b))));
        column.setMinWidth(90);
        table.getColumns().add(column);
        return column;
    }

    public static <T, K extends Comparable<? super K>> void column(TableView<T> table, String title,
                                                                   Function<? super T, K> key,
                                                                   Function<? super K, String> format) {
        TableColumn<T, K> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(key.apply(cell.getValue())));
        column.setComparator(Comparator.nullsFirst(Comparator.naturalOrder()));
        column.setCellFactory(c -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(K item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.apply(item));
            }
        });
        column.setMinWidth(90);
        table.getColumns().add(column);
    }

    public static <T> void setRows(TableView<T> table, List<T> rows) {
        table.getItems().setAll(rows);
    }

    public static <T> Optional<T> selected(TableView<T> table, String hint) {
        T item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            Dialogs.info(table, hint);
        }
        return Optional.ofNullable(item);
    }

    public static <T> void onDoubleClick(TableView<T> table, Consumer<T> action) {
        table.setRowFactory(t -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    public static void fitRows(TableView<?> table, int rows) {
        double height = HEADER_HEIGHT + Math.max(1, rows) * ROW_HEIGHT + 4;
        table.setMinHeight(height);
        table.setPrefHeight(height);
        table.setMaxHeight(height);
    }
}
