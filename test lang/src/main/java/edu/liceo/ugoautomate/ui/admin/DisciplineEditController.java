package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.model.OffenseSeverity;
import edu.liceo.ugoautomate.service.DisciplineService;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.Objects;

public class DisciplineEditController {
    private final AppContext ctx;
    private DisciplineRecord original;
    private boolean saved;

    @FXML private Label heading;
    @FXML private TextField studentNumber;
    @FXML private Button findButton;
    @FXML private Label studentName;
    @FXML private DatePicker incidentDate;
    @FXML private ComboBox<String> offense;
    @FXML private ComboBox<OffenseSeverity> severity;
    @FXML private TextArea description;
    @FXML private TextField sanction;
    @FXML private ComboBox<CaseStatus> status;
    @FXML private TextArea resolution;

    public DisciplineEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        offense.getItems().setAll(DisciplineService.COMMON_OFFENSES);
        severity.getItems().setAll(OffenseSeverity.values());
        severity.setValue(OffenseSeverity.MINOR);
        status.getItems().setAll(CaseStatus.values());
        status.setValue(CaseStatus.OPEN);
        incidentDate.setValue(LocalDate.now());
    }

    public void setRecord(DisciplineRecord record) {
        this.original = record;
        heading.setText(record == null ? "New Discipline Record" : "Edit Discipline Record");
        if (record == null) {
            return;
        }
        studentNumber.setText(record.getStudentNumber());
        studentNumber.setEditable(false);
        findButton.setDisable(true);
        studentName.setText(record.getStudentName());
        incidentDate.setValue(record.getIncidentDate());
        offense.setValue(record.getOffense());
        offense.getEditor().setText(record.getOffense());
        severity.setValue(record.getSeverity());
        description.setText(Objects.toString(record.getDescription(), ""));
        sanction.setText(Objects.toString(record.getSanction(), ""));
        status.setValue(record.getStatus());
        resolution.setText(Objects.toString(record.getResolution(), ""));
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void findStudent(ActionEvent event) {
        event.consume();
        String number = studentNumber.getText();
        Async.run(studentNumber, () -> ctx.discipline().findStudent(number), student ->
                studentName.setText(student.getFullName() + " - " + student.getSummary()));
    }

    @FXML
    private void save() {
        DisciplineRecord r = new DisciplineRecord();
        if (original != null) {
            r.setId(original.getId());
        }
        r.setIncidentDate(incidentDate.getValue());
        r.setOffense(offense.getEditor().getText());
        r.setSeverity(severity.getValue());
        r.setDescription(description.getText());
        r.setSanction(sanction.getText());
        r.setStatus(status.getValue());
        r.setResolution(resolution.getText());
        String number = studentNumber.getText();
        Async.run(studentNumber, () -> original == null ? ctx.discipline().create(number, r) : ctx.discipline().update(r),
                result -> {
                    saved = true;
                    Views.close(studentNumber);
                });
    }

    @FXML
    private void cancel() {
        Views.close(studentNumber);
    }
}
