package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Choice;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class DisciplineController implements Page {
    private static final String SELECT_HINT = "Select a case in the list first.";

    private record State(List<DisciplineRecord> rows, long openCases) {
    }

    private final AppContext ctx;

    @FXML private ComboBox<Choice<CaseStatus>> statusFilter;
    @FXML private TextField search;
    @FXML private Label count;
    @FXML private TableView<DisciplineRecord> table;
    @FXML private Label detailTitle;
    @FXML private Label detailText;

    public DisciplineController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Choice.fill(statusFilter, "All cases", List.of(CaseStatus.values()));

        Tables.setup(table, "No cases match the filter.");
        Tables.column(table, "Date", DisciplineRecord::getIncidentDate, DisplayTime::format);
        Tables.column(table, "Student ID", DisciplineRecord::getStudentNumber);
        Tables.column(table, "Student", DisciplineRecord::getStudentName);
        Tables.column(table, "Offense", DisciplineRecord::getOffense).setMinWidth(160);
        Tables.column(table, "Severity", DisciplineRecord::getSeverity);
        Tables.column(table, "Sanction", DisciplineRecord::getSanction).setMinWidth(160);
        Tables.column(table, "Status", DisciplineRecord::getStatus);
        Tables.onDoubleClick(table, this::openEditor);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> showDetails(selected));
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        CaseStatus status = Choice.selectedValue(statusFilter);
        String text = search.getText();
        Async.run(table, () -> new State(ctx.discipline().search(status, text), ctx.discipline().openCaseCount()), state -> {
            Tables.setRows(table, state.rows());
            count.setText(state.rows().size() + " case(s) shown  •  " + state.openCases() + " open in total");
            showDetails(null);
        });
    }

    private void showDetails(DisciplineRecord r) {
        if (r == null) {
            detailTitle.setText("Select a case to see its details.");
            detailText.setText("");
            return;
        }
        detailTitle.setText(r.getOffense() + " - " + r.getStudentName() + " (" + r.getStudentNumber() + ")");
        StringBuilder text = new StringBuilder();
        text.append("Incident date: ").append(DisplayTime.format(r.getIncidentDate()))
                .append("   Severity: ").append(r.getSeverity())
                .append("   Status: ").append(r.getStatus());
        text.append("\n\nWhat happened: ").append(Objects.toString(r.getDescription(), "(no description)"));
        text.append("\nSanction: ").append(Objects.toString(r.getSanction(), "(none)"));
        if (r.getStatus() == CaseStatus.RESOLVED) {
            text.append("\nResolved ").append(DisplayTime.format(r.getResolvedAt())).append(": ")
                    .append(Objects.toString(r.getResolution(), "(no notes)"));
        }
        text.append("\n\nRecorded by ").append(Objects.toString(r.getRecordedByName(), "(removed account)"))
                .append(" on ").append(DisplayTime.format(r.getRecordedAt()));
        detailText.setText(text.toString());
    }

    @FXML
    private void add() {
        openEditor(null);
    }

    @FXML
    private void edit() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::openEditor);
    }

    private void openEditor(DisciplineRecord record) {
        View<DisciplineEditController> view = Views.load(ctx, "admin/DisciplineEdit.fxml");
        view.controller().setRecord(record);
        Views.showDialog(table, record == null ? "New Discipline Record" : "Edit Discipline Record", view.root());
        if (view.controller().isSaved()) {
            reload();
        }
    }

    @FXML
    private void resolve() {
        Tables.selected(table, SELECT_HINT).ifPresent(record -> {
            if (record.getStatus() == CaseStatus.RESOLVED) {
                Dialogs.info(table, "This case is already resolved.");
                return;
            }
            TextArea notes = new TextArea();
            notes.setPromptText("How was the case settled? e.g. Sanction completed, parent conference held.");
            notes.setWrapText(true);
            notes.setPrefRowCount(4);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(AppConfig.APP_NAME);
            dialog.setHeaderText("Resolve the case of " + record.getStudentName() + " (" + record.getOffense() + ")");
            dialog.getDialogPane().setContent(notes);
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.initOwner(Views.windowOf(table));
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            record.setStatus(CaseStatus.RESOLVED);
            record.setResolution(notes.getText());

            Async.run(table, () -> ctx.discipline().update(record), saved -> { }, this::reload);
        });
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(record -> {
            if (!Dialogs.confirm(table, "Permanently delete the " + record.getOffense() + " case of "
                    + record.getStudentName() + "?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.discipline().delete(record.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
