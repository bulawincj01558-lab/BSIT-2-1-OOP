package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.EvaluationCriterion;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.EnumMap;
import java.util.Map;

public class EvaluationFormController {
    private final AppContext ctx;
    private final Map<EvaluationCriterion, ToggleGroup> answers = new EnumMap<>(EvaluationCriterion.class);
    private Faculty faculty;
    private boolean submitted;

    @FXML private Label facultyName;
    @FXML private Label details;
    @FXML private Label scale;
    @FXML private GridPane questions;
    @FXML private TextArea comment;
    @FXML private Button submitButton;

    public EvaluationFormController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StringBuilder legend = new StringBuilder("Rating scale: ");
        for (int r = EvaluationCriterion.MIN_RATING; r <= EvaluationCriterion.MAX_RATING; r++) {
            legend.append(r).append(" = ").append(EvaluationCriterion.ratingLabel(r))
                    .append(r < EvaluationCriterion.MAX_RATING ? ",  " : "");
        }
        scale.setText(legend.toString());

        int row = 0;
        for (EvaluationCriterion criterion : EvaluationCriterion.values()) {
            Label question = new Label((row + 1) + ". " + criterion.getQuestion());
            question.setWrapText(true);
            ToggleGroup group = new ToggleGroup();
            HBox choices = new HBox(14);
            for (int r = EvaluationCriterion.MIN_RATING; r <= EvaluationCriterion.MAX_RATING; r++) {
                RadioButton choice = new RadioButton(String.valueOf(r));
                choice.setUserData(r);
                choice.setToggleGroup(group);
                choice.setTooltip(new Tooltip(EvaluationCriterion.ratingLabel(r)));
                choices.getChildren().add(choice);
            }
            answers.put(criterion, group);
            questions.addRow(row++, question, choices);
        }
    }

    public void setFaculty(Faculty evaluated, String term) {
        this.faculty = evaluated;
        facultyName.setText("Evaluate " + evaluated.getFullName());
        details.setText(evaluated.getDepartment() + "  •  " + term);
    }

    public boolean isSubmitted() {
        return submitted;
    }

    @FXML
    private void submit() {
        Map<EvaluationCriterion, Integer> ratings = new EnumMap<>(EvaluationCriterion.class);
        answers.forEach((criterion, group) -> {
            Toggle selected = group.getSelectedToggle();
            if (selected != null) {
                ratings.put(criterion, (Integer) selected.getUserData());
            }
        });
        String text = comment.getText();
        submitButton.setDisable(true);
        Async.run(questions, () -> ctx.evaluations().submit(faculty.getId(), ratings, text), saved -> {
            submitted = true;
            Views.close(questions);
        }, () -> submitButton.setDisable(false));
    }

    @FXML
    private void cancel() {
        Views.close(questions);
    }
}
