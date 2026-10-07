package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.service.ProfileUpdate;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Forms;
import edu.liceo.ugoautomate.ui.common.StudentFields;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Objects;

public class AccountEditController {
    private final AppContext ctx;
    private User user;
    private boolean saved;

    @FXML private Label title;
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
    @FXML private CheckBox active;

    public AccountEditController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        StudentFields.setupCourse(course);
        StudentFields.setupYearLevel(yearLevel);
    }

    public void setUser(User account) {
        this.user = account;
        ProfileUpdate values = ProfileUpdate.of(account);
        title.setText(account.getFullName());
        email.setText(account.getEmail());
        accountType.setText(account.getRole().getLabel());
        firstName.setText(values.firstName());
        lastName.setText(values.lastName());
        contact.setText(Objects.toString(values.contactNumber(), ""));
        active.setSelected(account.isActive());

        Forms.row(form, "Email", email);
        Forms.row(form, "Account type", accountType);
        if (account instanceof Student s) {
            studentNumber.setText(s.getStudentNumber());
            Forms.row(form, "Student ID", studentNumber);
        }
        Forms.row(form, "First name", firstName);
        Forms.row(form, "Last name", lastName);
        Forms.row(form, "Contact number", contact);
        if (account instanceof Student s) {
            StudentFields.setCourse(course, s.getCourse());
            yearLevel.setValue(s.getYearLevel());
            Forms.row(form, "Course", course);
            Forms.row(form, "Year level", yearLevel);
        } else if (account instanceof Guest g) {
            purpose.setText(g.getPurposeOfVisit());
            personToVisit.setText(g.getPersonToVisit());
            Forms.row(form, "Purpose of visit", purpose);
            Forms.row(form, "Person/office to visit", personToVisit);
        } else if (account instanceof Administrator a) {
            position.setText(Objects.toString(a.getPosition(), ""));
            Forms.row(form, "Position", position);
        }
        Forms.row(form, "Status", active);
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void save() {
        ProfileUpdate update = new ProfileUpdate(firstName.getText(), lastName.getText(), contact.getText(),
                StudentFields.courseText(course), yearLevel.getValue(),
                purpose.getText(), personToVisit.getText(), position.getText());
        boolean isActive = active.isSelected();
        Async.run(form, () -> ctx.accounts().updateAccount(user.getId(), update, isActive), updated -> {
            saved = true;
            Views.close(form);
        });
    }

    @FXML
    private void cancel() {
        Views.close(form);
    }
}
