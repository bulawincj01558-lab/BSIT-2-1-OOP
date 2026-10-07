package edu.liceo.ugoautomate.ui.common;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public final class Forms {
    private Forms() {
    }

    public static void clear(GridPane form) {
        form.getChildren().clear();
    }

    public static void row(GridPane form, String label, Node field) {
        int row = form.getRowCount();
        GridPane.setHgrow(field, Priority.ALWAYS);
        form.addRow(row, new Label(label), field);
    }

    public static void section(GridPane form, String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add("section-title");
        form.add(heading, 0, form.getRowCount(), 2, 1);
    }
}
