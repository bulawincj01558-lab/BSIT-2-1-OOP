package edu.liceo.ugoautomate.ui.common;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

public final class Tabs {
    private Tabs() {
    }

    public static void link(ToggleButton[] buttons, Node[] panels) {
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < buttons.length; i++) {
            ToggleButton button = buttons[i];
            int index = i;
            button.setToggleGroup(group);
            button.getStyleClass().add("chip");
            button.setOnAction(e -> {
                button.setSelected(true);
                show(panels, index);
            });
        }
        buttons[0].setSelected(true);
        show(panels, 0);
    }

    private static void show(Node[] panels, int index) {
        for (int i = 0; i < panels.length; i++) {
            panels[i].setVisible(i == index);
            panels[i].setManaged(i == index);
        }
    }
}
