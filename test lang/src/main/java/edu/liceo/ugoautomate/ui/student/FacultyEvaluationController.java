package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.EvaluationPeriod;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Set;

public class FacultyEvaluationController implements Page {
    private record State(EvaluationPeriod period, List<Faculty> faculty, Set<Long> evaluatedIds) {
    }

    private final AppContext ctx;
    private State state;

    @FXML private Label periodLabel;
    @FXML private Label progressLabel;
    @FXML private Button evaluateButton;
    @FXML private TableView<Faculty> table;

    public FacultyEvaluationController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No faculty members to evaluate yet.");
        Tables.column(table, "Faculty", Faculty::getFullName);
        Tables.column(table, "Department", Faculty::getDepartment);
        Tables.column(table, "Status", f -> state != null && state.evaluatedIds().contains(f.getId()) ? "✔ Evaluated" : "Not yet");
        Tables.onDoubleClick(table, this::evaluate);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> new State(ctx.evaluations().currentPeriod(), ctx.evaluations().facultyToEvaluate(),
                ctx.evaluations().myEvaluatedFacultyIds()), loaded -> {
            state = loaded;
            EvaluationPeriod period = loaded.period();
            periodLabel.setText(period.term() + (period.open() ? " - open for evaluation" : " - evaluation is closed"));
            long done = loaded.faculty().stream().filter(f -> loaded.evaluatedIds().contains(f.getId())).count();
            progressLabel.setText("You have evaluated " + done + " of " + loaded.faculty().size() + " faculty member(s).");
            evaluateButton.setDisable(!period.open());
            Tables.setRows(table, loaded.faculty());
            table.refresh();
        });
    }

    @FXML
    private void evaluate() {
        Tables.selected(table, "Select a faculty member in the list first.").ifPresent(this::evaluate);
    }

    private void evaluate(Faculty faculty) {
        if (state == null) {
            return;
        }
        if (!state.period().open()) {
            Dialogs.info(table, "Faculty evaluation for " + state.period().term() + " is closed.");
            return;
        }
        if (state.evaluatedIds().contains(faculty.getId())) {
            Dialogs.info(table, "You already evaluated " + faculty.getFullName() + " for " + state.period().term() + ".");
            return;
        }
        View<EvaluationFormController> form = Views.load(ctx, "student/EvaluationForm.fxml");
        form.controller().setFaculty(faculty, state.period().term());
        Views.showDialog(table, "Evaluate " + faculty.getFullName(), form.root());
        if (form.controller().isSubmitted()) {
            Dialogs.info(table, "Thank you! Your evaluation of " + faculty.getFullName() + " was submitted.");
            reload();
        }
    }
}
