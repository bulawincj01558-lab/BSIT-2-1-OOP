package edu.liceo.ugoautomate.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(
            () -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));

    private Money() {
    }

    public static String format(long cents) {
        String amount = FORMAT.get().format(BigDecimal.valueOf(Math.abs(cents), 2));
        return (cents < 0 ? "-₱" : "₱") + amount;
    }

    public static long parse(String text, String field) {
        String cleaned = text == null ? "" : text.replace("₱", "").replace("P", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.scale() > 2) {
                throw new ValidationException(field + " can have at most 2 decimal places.");
            }
            if (value.signum() <= 0) {
                throw new ValidationException(field + " must be more than zero.");
            }
            return value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new ValidationException(field + " must be an amount such as 1,500.00.");
        }
    }

    public static String toInput(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
