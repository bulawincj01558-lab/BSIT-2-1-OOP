package edu.liceo.ugoautomate.ui.common;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class TimeSpinner extends Spinner<LocalTime> {
    private static final int STEP_MINUTES = 5;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final List<DateTimeFormatter> INPUT = List.of(
            DISPLAY,
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH));

    public TimeSpinner() {
        SpinnerValueFactory<LocalTime> factory = new SpinnerValueFactory<>() {
            @Override
            public void decrement(int steps) {
                setValue(getValue().minusMinutes((long) steps * STEP_MINUTES));
            }

            @Override
            public void increment(int steps) {
                setValue(getValue().plusMinutes((long) steps * STEP_MINUTES));
            }
        };
        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : DISPLAY.format(time);
            }

            @Override
            public LocalTime fromString(String text) {
                String cleaned = text.trim().toUpperCase(Locale.ENGLISH);
                for (DateTimeFormatter format : INPUT) {
                    try {
                        return LocalTime.parse(cleaned, format);
                    } catch (DateTimeParseException ignored) {
                    }
                }
                return factory.getValue();
            }
        });
        factory.setValue(LocalTime.of(8, 0));
        setValueFactory(factory);
        setEditable(true);

        focusedProperty().addListener((obs, was, is) -> {
            if (!is) {
                commitValue();
            }
        });
    }

    public void setTime(LocalTime time) {
        getValueFactory().setValue(time);
    }

    public LocalTime getTime() {
        commitValue();
        return getValue();
    }
}
