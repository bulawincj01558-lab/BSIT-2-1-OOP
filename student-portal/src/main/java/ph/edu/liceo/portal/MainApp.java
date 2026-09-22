package ph.edu.liceo.portal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ph.edu.liceo.portal.controller.ProfileController;
import ph.edu.liceo.portal.model.Student;
import ph.edu.liceo.portal.model.StudentDirectory;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage stage;
    private static final StudentDirectory directory = new StudentDirectory();

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Liceo Student Portal");
        stage.setResizable(false);
        showLogin();
        stage.show();
    }

    public static StudentDirectory getDirectory() {
        return directory;
    }

    public static void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login.fxml"));
            setScene(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void showProfile(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setStudent(student);
            setScene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setScene(Parent root) {
        Scene scene = new Scene(root, 380, 520);
        scene.getStylesheets().add(MainApp.class.getResource("style.css").toExternalForm());
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}