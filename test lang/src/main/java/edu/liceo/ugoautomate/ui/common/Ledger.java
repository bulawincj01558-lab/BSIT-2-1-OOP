package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.model.TuitionEntry;
import edu.liceo.ugoautomate.util.Money;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Ledger {
    public record Row(TuitionEntry entry, long balanceAfterCents) {
    }

    private Ledger() {
    }

    public static void setup(TableView<Row> table, String emptyMessage) {
        Tables.setup(table, emptyMessage);
        Tables.column(table, "Date", r -> r.entry().getEntryDate(), DisplayTime::format);
        Tables.column(table, "Description", r -> r.entry().getDescription()).setMinWidth(200);
        Tables.column(table, "Type", r -> r.entry().getType());
        Tables.column(table, "Amount", r -> r.entry().getSignedCents(), Ledger::amount);
        Tables.column(table, "Reference", r -> Objects.toString(r.entry().getReference(), ""));
        Tables.column(table, "Balance", Row::balanceAfterCents, Money::format);
    }

    public static List<Row> rows(List<TuitionEntry> entries) {
        List<Row> rows = new ArrayList<>();
        long balance = 0;
        for (TuitionEntry entry : entries) {
            balance += entry.getSignedCents();
            rows.add(new Row(entry, balance));
        }
        return rows;
    }

    private static String amount(long signedCents) {
        return (signedCents > 0 ? "+" : "") + Money.format(signedCents);
    }

    public static String balanceLabel(long balanceCents) {
        if (balanceCents > 0) {
            return Money.format(balanceCents) + " remaining balance";
        }
        if (balanceCents == 0) {
            return "Fully paid";
        }
        return Money.format(-balanceCents) + " credit (overpaid)";
    }

    public static String totals(long charges, long payments, long discounts) {
        return "Charges " + Money.format(charges)
                + "  •  Payments " + Money.format(payments)
                + "  •  Discounts " + Money.format(discounts);
    }
}
