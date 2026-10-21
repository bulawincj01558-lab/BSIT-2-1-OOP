package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.service.StudentRegistration;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Forms;
import edu.liceo.ugoautomate.ui.common.StudentFields;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class StudentAddController {
    private final AppContext ctx;
    private Student added;

    @FXML private GridPane form;
    @FXML private TextField studentNumber;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private ComboBox<String> course;
    @FXML private ComboBox<Integer> yearLevel;
    @FXML private TextField email;
    @FXML private TextField contact;
    @FXML private PasswordField password;
    @FXML private PasswordField confirmPassword;

    public StudentAddController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StudentFields.setupCourse(course);
        StudentFields.setupYearLevel(yearLevel);

        Forms.section(form, "Student");
        Forms.row(form, "Student ID", studentNumber);
        Forms.row(form, "First name", firstName);
        Forms.row(form, "Last name", lastName);
        Forms.row(form, "Course", course);
        Forms.row(form, "Year level", yearLevel);
        Forms.section(form, "Account");
        Forms.row(form, "Email", email);
        Forms.row(form, "Contact number (optional)", contact);
        Forms.row(form, "Password", password);
        Forms.row(form, "Confirm password", confirmPassword);
    }

    public Optional<Student> added() {
        return Optional.ofNullable(added);
    }

    @FXML
    private void save() {
        Integer year = yearLevel.getValue();
        StudentRegistration registration = new StudentRegistration(
                studentNumber.getText(), firstName.getText(), lastName.getText(),
                StudentFields.courseText(course), year == null ? 0 : year,
                email.getText(), contact.getText(), password.getText(), confirmPassword.getText());
        Async.run(form, () -> ctx.accounts().addStudent(registration), student -> {
            added = student;
            Views.close(form);
        });
    }

    @FXML
    private void cancel() {
        Views.close(form);
    }
}
