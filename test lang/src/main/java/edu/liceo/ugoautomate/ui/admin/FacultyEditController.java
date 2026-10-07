package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;

public class FacultyEditController {
    private final AppContext ctx;
    private Faculty original;
    private boolean saved;

    @FXML private Label heading;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private ComboBox<String> department;
    @FXML private TextField email;
    @FXML private CheckBox active;

    public FacultyEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    public void setFaculty(Faculty faculty, List<Faculty> allFaculty) {
        this.original = faculty;
        heading.setText(faculty == null ? "Add Faculty" : "Edit Faculty");
        allFaculty.stream().map(Faculty::getDepartment).distinct().sorted().forEach(department.getItems()::add);
        if (faculty != null) {
            firstName.setText(faculty.getFirstName());
            lastName.setText(faculty.getLastName());
            department.setValue(faculty.getDepartment());
            department.getEditor().setText(faculty.getDepartment());
            email.setText(Objects.toString(faculty.getEmail(), ""));
            active.setSelected(faculty.isActive());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        Faculty f = new Faculty();
        if (original != null) {
            f.setId(original.getId());
        }
        f.setFirstName(firstName.getText());
        f.setLastName(lastName.getText());
        f.setDepartment(department.getEditor().getText());
        f.setEmail(email.getText());
        f.setActive(active.isSelected());
        Async.run(firstName, () -> original == null ? ctx.faculty().create(f) : ctx.faculty().update(f), result -> {
            saved = true;
            Views.close(firstName);
        });
    }

    @FXML
    private void cancel() {
        Views.close(firstName);
    }
}
