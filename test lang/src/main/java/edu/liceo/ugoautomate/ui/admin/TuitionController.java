package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.TuitionAccount;
import edu.liceo.ugoautomate.model.TuitionEntryType;
import edu.liceo.ugoautomate.model.TuitionStatement;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Ledger;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.Money;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class TuitionController implements Page {
    private final AppContext ctx;
    private boolean loadingTerms;

    private Long shownStudentId;

    @FXML private ComboBox<String> termFilter;
    @FXML private TextField search;
    @FXML private CheckBox withBalanceOnly;
    @FXML private Label summary;
    @FXML private TableView<TuitionAccount> accounts;
    @FXML private Label ledgerTitle;
    @FXML private Label ledgerTotals;
    @FXML private TableView<Ledger.Row> ledger;

    public TuitionController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(accounts, "No students match the search.");
        Tables.column(accounts, "Student ID", TuitionAccount::studentNumber);
        Tables.column(accounts, "Student", TuitionAccount::name).setMinWidth(150);
        Tables.column(accounts, "Course", a -> a.course() + " " + a.yearLevel()).setMinWidth(160);
        Tables.column(accounts, "Charges", TuitionAccount::chargesCents, Money::format);
        Tables.column(accounts, "Payments", TuitionAccount::paymentsCents, Money::format);
        Tables.column(accounts, "Discounts", TuitionAccount::discountsCents, Money::format);
        Tables.column(accounts, "Balance", TuitionAccount::balanceCents, Money::format);
        accounts.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                shownStudentId = selected.studentId();
                loadLedger(selected);
            }
        });
        Ledger.setup(ledger, "No entries for this student in this term.");
    }

    @Override
    public void onShow() {
        String keep = termFilter.getValue();
        Async.run(accounts, () -> ctx.tuition().terms(), terms -> {
            loadingTerms = true;
            termFilter.getItems().setAll(terms);
            termFilter.setValue(keep != null && terms.contains(keep) ? keep : terms.get(0));
            loadingTerms = false;
            reload();
        });
    }

    @FXML
    private void reload() {
        String term = termFilter.getValue();
        if (loadingTerms || term == null) {
            return;
        }
        String text = search.getText();
        boolean onlyOwing = withBalanceOnly.isSelected();
        Async.run(accounts, () -> ctx.tuition().accounts(term, text), all -> {
            List<TuitionAccount> rows = onlyOwing ? all.stream().filter(a -> a.balanceCents() > 0).toList() : all;
            long receivable = all.stream().mapToLong(TuitionAccount::balanceCents).filter(b -> b > 0).sum();
            long owing = all.stream().filter(a -> a.balanceCents() > 0).count();
            summary.setText(rows.size() + " student(s) shown  •  " + owing + " with a balance  •  "
                    + Money.format(receivable) + " total receivable");
            Tables.setRows(accounts, rows);
            rows.stream().filter(a -> shownStudentId != null && a.studentId() == shownStudentId).findFirst()
                    .ifPresentOrElse(a -> accounts.getSelectionModel().select(a), this::clearLedger);
        });
    }

    private void loadLedger(TuitionAccount account) {
        String term = termFilter.getValue();
        ledgerTitle.setText(account.name() + " (" + account.studentNumber() + ") - " + Ledger.balanceLabel(account.balanceCents()));
        ledgerTotals.setText(Ledger.totals(account.chargesCents(), account.paymentsCents(), account.discountsCents()));
        Async.run(ledger, () -> ctx.tuition().statement(account.studentId(), term),
                (TuitionStatement statement) -> Tables.setRows(ledger, Ledger.rows(statement.entries())));
    }

    private void clearLedger() {
        shownStudentId = null;
        ledgerTitle.setText("Select a student to see their ledger.");
        ledgerTotals.setText("");
        ledger.getItems().clear();
    }

    @FXML
    private void addCharge() {
        openEntryForm(TuitionEntryType.CHARGE);
    }

    @FXML
    private void addPayment() {
        openEntryForm(TuitionEntryType.PAYMENT);
    }

    @FXML
    private void addDiscount() {
        openEntryForm(TuitionEntryType.DISCOUNT);
    }

    private void openEntryForm(TuitionEntryType type) {
        Tables.selected(accounts, "Select a student in the list first.").ifPresent(account -> {
            View<TuitionEntryController> view = Views.load(ctx, "admin/TuitionEntry.fxml");
            view.controller().setUp(account, termFilter.getValue(), type);
            Views.showDialog(accounts, type.getLabel() + " - " + account.name(), view.root());
            if (view.controller().isSaved()) {
                reload();
            }
        });
    }

    @FXML
    private void deleteEntry() {
        Tables.selected(ledger, "Select a ledger entry first.").ifPresent(row -> {
            if (!Dialogs.confirm(ledger, "Delete \"" + row.entry().getDescription() + "\" ("
                    + Money.format(row.entry().getAmountCents()) + ")?\nOnly delete entries recorded by mistake.")) {
                return;
            }
            Async.run(ledger, () -> {
                ctx.tuition().deleteEntry(row.entry().getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }

    @FXML
    private void assess() {
        String term = termFilter.getValue();
        View<TuitionAssessController> view = Views.load(ctx, "admin/TuitionAssess.fxml");
        view.controller().setTerm(term);
        Views.showDialog(accounts, "Assess Tuition", view.root());
        if (view.controller().isDone()) {
            reload();
        }
    }
}
