package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Objects;

public class MyDisciplineController implements Page {
    private final AppContext ctx;

    @FXML private Label summary;
    @FXML private TableView<DisciplineRecord> table;
    @FXML private Label detailTitle;
    @FXML private Label detailText;

    public MyDisciplineController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No disciplinary cases. Keep it up!");
        Tables.column(table, "Date", DisciplineRecord::getIncidentDate, DisplayTime::format);
        Tables.column(table, "Offense", DisciplineRecord::getOffense).setMinWidth(160);
        Tables.column(table, "Severity", DisciplineRecord::getSeverity);
        Tables.column(table, "Sanction", DisciplineRecord::getSanction).setMinWidth(160);
        Tables.column(table, "Status", DisciplineRecord::getStatus);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showDetails(selected));
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> ctx.discipline().myRecord(), records -> {
            Tables.setRows(table, records);
            summary.setText(summaryText(records));
            showDetails(null);
        });
    }

    private static String summaryText(List<DisciplineRecord> records) {
        if (records.isEmpty()) {
            return "You have a clean record. Keep it up!";
        }
        long open = records.stream().filter(r -> r.getStatus() == CaseStatus.OPEN).count();
        return records.size() + " case(s) on record, " + open + " still open.";
    }

    private void showDetails(DisciplineRecord r) {
        if (r == null) {
            detailTitle.setText("Select a case to see its details.");
            detailText.setText("");
            return;
        }
        detailTitle.setText(r.getOffense() + " (" + DisplayTime.format(r.getIncidentDate()) + ")");
        StringBuilder text = new StringBuilder("Severity: ").append(r.getSeverity())
                .append("   Status: ").append(r.getStatus());
        text.append("\n\nWhat happened: ").append(Objects.toString(r.getDescription(), "(no description)"));
        text.append("\nSanction: ").append(Objects.toString(r.getSanction(), "(none)"));
        if (r.getStatus() == CaseStatus.RESOLVED) {
            text.append("\nResolved ").append(DisplayTime.format(r.getResolvedAt())).append(": ")
                    .append(Objects.toString(r.getResolution(), "(no notes)"));
        }
        detailText.setText(text.toString());
    }
}
