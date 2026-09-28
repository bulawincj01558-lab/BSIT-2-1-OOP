package edu.liceo.ugoautomate.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Conversions between Java time types, database text, and display strings.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DB_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private DateTimeUtil() {
    }

    public static String toDb(LocalDateTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.SECONDS).format(DB_DATE_TIME);
    }

    public static LocalDateTime fromDb(String text) {
        return text == null || text.isBlank() ? null : LocalDateTime.parse(text, DB_DATE_TIME);
    }

    public static String toDb(LocalDate date) {
        return date == null ? null : date.toString();
    }

    public static LocalDate dateFromDb(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text);
    }

    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DISPLAY_DATE_TIME);
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    public static String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DISPLAY_TIME);
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /** @return 23:59:59 on the given date */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).truncatedTo(ChronoUnit.SECONDS);
    }
}
