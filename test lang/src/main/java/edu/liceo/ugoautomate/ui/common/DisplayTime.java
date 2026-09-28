package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.util.DateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Table cell value that displays a friendly date/time but sorts chronologically.
 */
public record DisplayTime(LocalDateTime time, boolean dateOnly) implements Comparable<DisplayTime> {

    private static final Comparator<LocalDateTime> ORDER = Comparator.nullsFirst(Comparator.naturalOrder());

    public static DisplayTime of(LocalDateTime time) {
        return new DisplayTime(time, false);
    }

    public static DisplayTime of(LocalDate date) {
        return new DisplayTime(date == null ? null : date.atStartOfDay(), true);
    }

    @Override
    public int compareTo(DisplayTime other) {
        return ORDER.compare(time, other.time);
    }

    @Override
    public String toString() {
        if (time == null) {
            return "";
        }
        return dateOnly ? DateTimeUtil.format(time.toLocalDate()) : DateTimeUtil.format(time);
    }
}
