package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.service.ProfileUpdate;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Objects;

public class ProfileController implements Page {
    private final AppContext ctx;
    private Runnable onSaved = () -> { };
    private User user;

    @FXML private GridPane columns;
    @FXML private GridPane form;
    @FXML private TextField email;
    @FXML private TextField accountType;
    @FXML private TextField studentNumber;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField contact;
    @FXML private ComboBox<String> course;
    @FXML private ComboBox<Integer> yearLevel;
    @FXML private TextField purpose;
    @FXML private TextField personToVisit;
    @FXML private TextField position;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmPassword;

    public ProfileController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StudentFields.setupCourse(course);
        StudentFields.setupYearLevel(yearLevel);

        Responsive.split(columns, 760, 0.55);
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @Override
    public void onShow() {
        clearPasswords();
        Async.run(form, () -> ctx.profiles().myProfile(), this::fill);
    }

    private void fill(User loaded) {
        user = loaded;
        ProfileUpdate values = ProfileUpdate.of(loaded);
        email.setText(loaded.getEmail());
        accountType.setText(loaded.getRole().getLabel());
        firstName.setText(values.firstName());
        lastName.setText(values.lastName());
        contact.setText(Objects.toString(values.contactNumber(), ""));

        Forms.clear(form);
        Forms.section(form, "Account");
        Forms.row(form, "Email", email);
        Forms.row(form, "Account type", accountType);
        if (loaded instanceof Student s) {
            studentNumber.setText(s.getStudentNumber());
            Forms.row(form, "Student ID", studentNumber);
        }
        Forms.section(form, "Personal information");
        Forms.row(form, "First name", firstName);
        Forms.row(form, "Last name", lastName);
        Forms.row(form, loaded instanceof Guest ? "Contact number" : "Contact number (optional)", contact);
        if (loaded instanceof Student s) {
            StudentFields.setCourse(course, s.getCourse());
            yearLevel.setValue(s.getYearLevel());
            Forms.row(form, "Course", course);
            Forms.row(form, "Year level", yearLevel);
        } else if (loaded instanceof Guest g) {
            purpose.setText(g.getPurposeOfVisit());
            personToVisit.setText(g.getPersonToVisit());
            Forms.row(form, "Usual purpose of visit", purpose);
            Forms.row(form, "Person/office to visit", personToVisit);
        } else if (loaded instanceof Administrator a) {
            position.setText(Objects.toString(a.getPosition(), ""));
            Forms.row(form, "Position", position);
        }
    }

    @FXML
    private void save() {
        if (user == null) {
            return;
        }
        ProfileUpdate update = new ProfileUpdate(
                firstName.getText(), lastName.getText(), contact.getText(),
                StudentFields.courseText(course), yearLevel.getValue(),
                purpose.getText(), personToVisit.getText(), position.getText());
        Async.run(form, () -> ctx.profiles().updateMyProfile(update), updated -> {
            Dialogs.info(form, "Your profile has been updated.");
            onSaved.run();
            fill(updated);
        });
    }

    @FXML
    private void changePassword() {
        String current = currentPassword.getText();
        String next = newPassword.getText();
        String confirm = confirmPassword.getText();
        Async.run(form, () -> {
            ctx.profiles().changeMyPassword(current, next, confirm);
            return Boolean.TRUE;
        }, ok -> {
            clearPasswords();
            Dialogs.info(form, "Your password has been changed.");
        });
    }

    private void clearPasswords() {
        currentPassword.clear();
        newPassword.clear();
        confirmPassword.clear();
    }
}
