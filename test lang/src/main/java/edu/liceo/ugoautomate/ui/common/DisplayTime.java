package edu.liceo.ugoautomate.ui.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DisplayTime {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm:ss a");

    private DisplayTime() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String formatTime(LocalDateTime value) {
        return value == null ? "" : TIME.format(value);
    }

    public static String format(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String format(Instant value) {
        return value == null ? "" : DATE_TIME.format(LocalDateTime.ofInstant(value, ZoneId.systemDefault()));
    }
}
