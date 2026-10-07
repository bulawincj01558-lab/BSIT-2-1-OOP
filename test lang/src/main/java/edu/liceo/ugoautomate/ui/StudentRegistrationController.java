package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.service.StudentRegistration;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.StudentFields;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class StudentRegistrationController {
    private final AppContext ctx;
    private Consumer<String> onRegistered = email -> { };

    @FXML private TextField studentNumber;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private ComboBox<String> course;
    @FXML private ComboBox<Integer> yearLevel;
    @FXML private TextField email;
    @FXML private TextField contact;
    @FXML private PasswordField password;
    @FXML private PasswordField confirm;
    @FXML private Button registerButton;

    public StudentRegistrationController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StudentFields.setupCourse(course);
        StudentFields.setupYearLevel(yearLevel);
    }

    public void setOnRegistered(Consumer<String> onRegistered) {
        this.onRegistered = onRegistered;
    }

    @FXML
    private void register() {
        StudentRegistration form = new StudentRegistration(
                studentNumber.getText(), firstName.getText(), lastName.getText(),
                StudentFields.courseText(course), yearLevel.getValue(),
                email.getText(), contact.getText(), password.getText(), confirm.getText());
        registerButton.setDisable(true);
        Async.run(registerButton, () -> ctx.auth().registerStudent(form), student -> {
            Views.close(registerButton);
            onRegistered.accept(student.getEmail());
        }, () -> registerButton.setDisable(false));
    }

    @FXML
    private void cancel() {
        Views.close(registerButton);
    }
}
