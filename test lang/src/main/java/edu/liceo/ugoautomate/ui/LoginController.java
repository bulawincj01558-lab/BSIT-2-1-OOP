package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.Responsive;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class LoginController {
    private final AppContext ctx;

    @FXML private ScrollPane scroller;
    @FXML private GridPane split;
    @FXML private Label university;
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private Label message;
    @FXML private Button loginButton;

    public LoginController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        university.setText(AppConfig.UNIVERSITY_NAME);
        Responsive.split(split, 700, 0.45);
        Responsive.fillViewport(scroller);
    }

    void focus() {
        email.requestFocus();
    }

    @FXML
    private void login() {
        String emailText = email.getText();
        String passwordText = password.getText();
        loginButton.setDisable(true);
        showMessage("Logging in...", false);

        Async.<Object>run(loginButton, () -> {
            try {
                return ctx.auth().login(emailText, passwordText);
            } catch (RuntimeException e) {
                return e;
            }
        }, result -> {
            if (result instanceof User user) {
                Navigator.showDashboard(user);
            } else {
                showMessage(Dialogs.userMessage((RuntimeException) result), false);
                password.clear();
                password.requestFocus();
            }
        }, () -> loginButton.setDisable(false));
    }

    @FXML
    private void registerStudent() {
        View<StudentRegistrationController> view = Views.load(ctx, "StudentRegistration.fxml");
        view.controller().setOnRegistered(this::prefillEmail);
        Views.showDialog(loginButton, "Student Registration", view.root());
    }

    @FXML
    private void registerGuest() {
        View<GuestRegistrationController> view = Views.load(ctx, "GuestRegistration.fxml");
        view.controller().setOnRegistered(this::prefillEmail);
        Views.showDialog(loginButton, "Guest Registration", view.root());
    }

    private void prefillEmail(String registeredEmail) {
        email.setText(registeredEmail);
        password.clear();
        password.requestFocus();
        showMessage("Registration successful. You can now log in.", true);
    }

    private void showMessage(String text, boolean success) {
        message.setText(text);
        message.getStyleClass().setAll("label", success ? "success-text" : "error-text");
    }
}
