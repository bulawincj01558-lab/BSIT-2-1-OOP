package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.EvaluationCriterion;
import edu.liceo.ugoautomate.model.EvaluationPeriod;
import edu.liceo.ugoautomate.model.EvaluationSummary;
import edu.liceo.ugoautomate.model.FacultyEvaluation;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class EvaluationResultsController implements Page {
    private record State(EvaluationPeriod period, List<String> terms) {
    }

    private final AppContext ctx;
    private boolean loadingTerms;

    @FXML private TextField periodTerm;
    @FXML private CheckBox periodOpen;
    @FXML private ComboBox<String> termFilter;
    @FXML private TableView<EvaluationSummary> summaryTable;
    @FXML private Label commentsTitle;
    @FXML private TableView<FacultyEvaluation> commentTable;

    public EvaluationResultsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(summaryTable, "No faculty members yet.");
        Tables.column(summaryTable, "Faculty", s -> s.getFaculty().getFullName()).setMinWidth(170);
        Tables.column(summaryTable, "Department", s -> s.getFaculty().getDepartment()).setMinWidth(200);
        Tables.column(summaryTable, "Evaluations", EvaluationSummary::getCount);
        for (EvaluationCriterion criterion : EvaluationCriterion.values()) {
            Tables.column(summaryTable, criterion.getShortLabel(), s -> s.getAverage(criterion), EvaluationResultsController::score);
        }
        Tables.column(summaryTable, "Overall", EvaluationSummary::getOverall, EvaluationResultsController::score);
        summaryTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> loadComments(selected));

        Tables.setup(commentTable, "No evaluations for this faculty member in this term.");
        Tables.column(commentTable, "Submitted", FacultyEvaluation::getSubmittedAt, DisplayTime::format);
        Tables.column(commentTable, "Average", FacultyEvaluation::getAverage, EvaluationResultsController::score);
        Tables.column(commentTable, "Comment", e -> Objects.toString(e.getComment(), "(no comment)"));
    }

    private static String score(Double value) {
        return String.format("%.2f", value);
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void refresh() {
        String keep = termFilter.getValue();
        Async.run(summaryTable, () -> new State(ctx.evaluations().currentPeriod(), ctx.evaluations().terms()), state -> {
            periodTerm.setText(state.period().term());
            periodOpen.setSelected(state.period().open());
            loadingTerms = true;
            termFilter.getItems().setAll(state.terms());
            termFilter.setValue(keep != null && state.terms().contains(keep) ? keep : state.period().term());
            loadingTerms = false;
            loadSummaries();
        });
    }

    @FXML
    private void loadSummaries() {
        String term = termFilter.getValue();
        if (loadingTerms || term == null) {
            return;
        }
        Async.run(summaryTable, () -> ctx.evaluations().summaries(term), rows -> {
            Tables.setRows(summaryTable, rows);
            commentTable.getItems().clear();
            commentsTitle.setText("Select a faculty member to see their comments.");
        });
    }

    private void loadComments(EvaluationSummary selected) {
        String term = termFilter.getValue();
        if (selected == null || term == null) {
            return;
        }
        commentsTitle.setText("Evaluations of " + selected.getFaculty().getFullName() + " (" + term + ")");
        Async.run(commentTable, () -> ctx.evaluations().evaluationsOf(selected.getFaculty().getId(), term),
                rows -> Tables.setRows(commentTable, rows));
    }

    @FXML
    private void savePeriod() {
        String term = periodTerm.getText();
        boolean open = periodOpen.isSelected();
        Async.run(summaryTable, () -> ctx.evaluations().setPeriod(term, open), period -> {
            Dialogs.info(summaryTable, "Evaluation period saved: " + period.term()
                    + (period.open() ? " (open for students)." : " (closed)."));
            termFilter.setValue(null);
            refresh();
        });
    }
}
