package edu.liceo.ugoautomate.ui.common;

import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.Objects;

public record Choice<T>(String label, T value) {
    @Override
    public String toString() {
        return label;
    }

    public static <T> void fill(ComboBox<Choice<T>> box, String allLabel, List<T> values) {
        box.getItems().setAll(new Choice<>(allLabel, null));
        values.forEach(v -> box.getItems().add(new Choice<>(String.valueOf(v), v)));
        box.getSelectionModel().selectFirst();
    }

    public static <T> T selectedValue(ComboBox<Choice<T>> box) {
        Choice<T> choice = box.getValue();
        return choice == null ? null : choice.value();
    }

    public static <T> void select(ComboBox<Choice<T>> box, T value) {
        box.getItems().stream()
                .filter(c -> Objects.equals(c.value(), value))
                .findFirst()
                .ifPresent(box::setValue);
    }
}
