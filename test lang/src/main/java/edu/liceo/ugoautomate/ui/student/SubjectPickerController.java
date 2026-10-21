package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Page;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;

import java.util.Objects;

public class SubjectPickerController implements Page {
    private final AppContext ctx;
    private volatile ClassOffering selected;

    @FXML private ComboBox<ClassOffering> subjects;
    @FXML private Label status;

    public SubjectPickerController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        subjects.setCellFactory(list -> new SubjectCell());
        subjects.setButtonCell(new SubjectCell());
        subjects.valueProperty().addListener((obs, old, now) -> selected = now);
    }

    public ClassOffering selected() {
        return selected;
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Long keep = selected == null ? null : selected.getId();
        Async.run(subjects, () -> ctx.classPrograms().myClassProgram(), classes -> {
            subjects.getItems().setAll(classes);
            classes.stream().filter(c -> Objects.equals(c.getId(), keep)).findFirst()
                    .or(() -> classes.stream().findFirst())
                    .ifPresentOrElse(subjects::setValue, () -> subjects.setValue(null));
            status.setText(classes.isEmpty()
                    ? "You have no subjects in your class program yet."
                    : "When you upload or paste your own attendance QR, you are marked present in today's class of this subject.");
        });
    }

    private static final class SubjectCell extends ListCell<ClassOffering> {
        @Override
        protected void updateItem(ClassOffering c, boolean empty) {
            super.updateItem(c, empty);
            setText(empty || c == null ? null : c.getSubjectCode() + " - " + c.getSubjectTitle());
        }
    }
}
