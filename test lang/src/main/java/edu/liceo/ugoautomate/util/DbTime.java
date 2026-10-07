package edu.liceo.ugoautomate.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DbTime {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DbTime() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME.format(value);
    }

    public static String format(LocalDate value) {
        return value == null ? null : DATE.format(value);
    }

    public static LocalDateTime parseDateTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, DATE_TIME);
    }

    public static LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value, DATE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
