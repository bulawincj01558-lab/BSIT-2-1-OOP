package edu.liceo.ugoautomate.ui.common;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * Date and date-time spinners converting to and from {@code java.time}.
 */
public final class SwingDates {

    private SwingDates() {
    }

    public static JSpinner dateTimeSpinner(LocalDateTime initial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(toDate(initial), null, null, Calendar.MINUTE));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd  hh:mm a"));
        return spinner;
    }

    public static JSpinner dateSpinner(LocalDate initial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(toDate(initial.atStartOfDay()), null, null,
                Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    public static LocalDateTime getDateTime(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.MINUTES);
    }

    public static LocalDate getDate(JSpinner spinner) {
        return getDateTime(spinner).toLocalDate();
    }

    public static void setDateTime(JSpinner spinner, LocalDateTime value) {
        spinner.setValue(toDate(value));
    }

    private static Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
