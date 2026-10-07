package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.TuitionEntryType;
import edu.liceo.ugoautomate.model.TuitionStatement;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Ledger;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class MyTuitionController implements Page {
    private final AppContext ctx;

    @FXML private Label termLabel;
    @FXML private Label balance;
    @FXML private Label totals;
    @FXML private TableView<Ledger.Row> table;

    public MyTuitionController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Ledger.setup(table, "No charges or payments recorded for you this term.");
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> ctx.tuition().myStatement(), this::show);
    }

    private void show(TuitionStatement statement) {
        termLabel.setText(statement.term());
        balance.setText(statement.entries().isEmpty() ? "No tuition assessed yet" : Ledger.balanceLabel(statement.balanceCents()));
        totals.setText(Ledger.totals(statement.totalCents(TuitionEntryType.CHARGE),
                statement.totalCents(TuitionEntryType.PAYMENT), statement.totalCents(TuitionEntryType.DISCOUNT)));
        Tables.setRows(table, Ledger.rows(statement.entries()));
    }
}
