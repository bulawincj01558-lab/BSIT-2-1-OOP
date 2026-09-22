package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ph.edu.liceo.portal.MainApp;
import ph.edu.liceo.portal.model.Student;

public class LoginController {

    @FXML private TextField studentNoField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin() {
        String studentNo = studentNoField.getText().trim();
        String password = passwordField.getText();

        if (studentNo.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in both fields.");
            return;
        }

        Student student = MainApp.getDirectory().login(studentNo, password);
        if (student == null) {
            messageLabel.setText("Wrong student number or password.");
            passwordField.clear();
        } else {
            MainApp.showProfile(student);
        }
    }
}