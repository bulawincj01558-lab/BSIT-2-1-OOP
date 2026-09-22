package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ph.edu.liceo.portal.MainApp;
import ph.edu.liceo.portal.model.Student;

public class ProfileController {

    @FXML private Label initialsLabel;
    @FXML private Label nameLabel;
    @FXML private Label studentNoLabel;
    @FXML private Label courseLabel;
    @FXML private Label emailLabel;

    public void setStudent(Student student) {
        String[] parts = student.getFullName().trim().split("\\s+");
        String initials = "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0);

        initialsLabel.setText(initials.toUpperCase());
        nameLabel.setText(student.getFullName());
        studentNoLabel.setText(student.getStudentNo());
        courseLabel.setText(student.getCourse() + " - Year " + student.getYearLevel());
        emailLabel.setText(student.getEmail());
    }

    @FXML
    private void handleLogout() {
        MainApp.showLogin();
    }
}