package edu.liceo.ugoautomate.ui.common;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

public final class Responsive {
    private Responsive() {
    }

    public static void split(GridPane grid, double breakpoint, double firstShare) {
        Node first = grid.getChildren().get(0);
        Node second = grid.getChildren().get(1);
        ColumnConstraints left = new ColumnConstraints();
        left.setHgrow(Priority.ALWAYS);
        left.setMinWidth(0);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        right.setMinWidth(0);
        grid.getColumnConstraints().setAll(left, right);
        RowConstraints top = new RowConstraints();
        top.setVgrow(Priority.ALWAYS);
        RowConstraints bottom = new RowConstraints();
        bottom.setVgrow(Priority.NEVER);
        grid.getRowConstraints().setAll(top, bottom);

        grid.widthProperty().addListener((obs, old, width) -> {
            boolean wide = width.doubleValue() >= breakpoint;
            GridPane.setConstraints(first, 0, 0, wide ? 1 : 2, 1);
            GridPane.setConstraints(second, wide ? 1 : 0, wide ? 0 : 1, wide ? 1 : 2, 1);
            left.setPercentWidth(wide ? firstShare * 100 : 100);
            right.setPercentWidth(wide ? (1 - firstShare) * 100 : 0);

            bottom.setVgrow(wide ? Priority.NEVER : Priority.ALWAYS);
        });
        GridPane.setConstraints(first, 0, 0);
        GridPane.setConstraints(second, 1, 0);
        left.setPercentWidth(firstShare * 100);
        right.setPercentWidth((1 - firstShare) * 100);
    }

    public static void fillViewport(ScrollPane scroller) {
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(false);
        if (scroller.getContent() instanceof Region content) {
            content.minHeightProperty().bind(Bindings.createDoubleBinding(
                    () -> scroller.getViewportBounds().getHeight(), scroller.viewportBoundsProperty()));
        }
    }
}
