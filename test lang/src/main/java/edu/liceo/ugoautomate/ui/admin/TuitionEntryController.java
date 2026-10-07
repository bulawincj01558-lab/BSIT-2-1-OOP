package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.TuitionAccount;
import edu.liceo.ugoautomate.model.TuitionEntryType;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Ledger;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.util.Money;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TuitionEntryController {
    private static final Map<TuitionEntryType, List<String>> SUGGESTIONS = Map.of(
            TuitionEntryType.CHARGE, List.of("Tuition fee", "Miscellaneous fees", "Laboratory fee",
                    "Late enrollment fee", "Other fee"),
            TuitionEntryType.PAYMENT, List.of("Payment - Cash", "Payment - GCash", "Payment - Bank deposit",
                    "Payment - Check"),
            TuitionEntryType.DISCOUNT, List.of("Academic scholarship", "Athletic scholarship", "Sibling discount",
                    "Full payment discount", "Employee dependent discount"));

    private final AppContext ctx;
    private TuitionAccount account;
    private String term;
    private TuitionEntryType type;
    private boolean saved;

    @FXML private Label heading;
    @FXML private Label details;
    @FXML private DatePicker date;
    @FXML private ComboBox<String> description;
    @FXML private TextField amount;
    @FXML private Label referenceLabel;
    @FXML private TextField reference;

    public TuitionEntryController(AppContext ctx) {
        this.ctx = ctx;
    }

    public void setUp(TuitionAccount forAccount, String forTerm, TuitionEntryType entryType) {
        this.account = forAccount;
        this.term = forTerm;
        this.type = entryType;
        heading.setText(switch (entryType) {
            case CHARGE -> "Add charge";
            case PAYMENT -> "Record payment";
            case DISCOUNT -> "Add discount / scholarship";
        });
        details.setText(forAccount.name() + " (" + forAccount.studentNumber() + ")  •  " + forTerm
                + "\nCurrent: " + Ledger.balanceLabel(forAccount.balanceCents()));
        date.setValue(LocalDate.now());
        description.getItems().setAll(SUGGESTIONS.get(entryType));
        description.getSelectionModel().selectFirst();
        if (entryType == TuitionEntryType.PAYMENT) {
            referenceLabel.setText("OR number");
            reference.setPromptText("Official receipt number");
            if (forAccount.balanceCents() > 0) {
                amount.setText(Money.toInput(forAccount.balanceCents()));
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        LocalDate day = date.getValue();
        String text = description.getEditor().getText();
        String ref = reference.getText();
        String amountText = amount.getText();
        Async.run(amount, () -> ctx.tuition().addEntry(account.studentId(), term, type, text,
                Money.parse(amountText, "Amount"), ref, day), entry -> {
            saved = true;
            Views.close(amount);
        });
    }

    @FXML
    private void cancel() {
        Views.close(amount);
    }
}
