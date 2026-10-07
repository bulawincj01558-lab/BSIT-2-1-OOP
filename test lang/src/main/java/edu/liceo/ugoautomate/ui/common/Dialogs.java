package edu.liceo.ugoautomate.ui.common;

import edu.liceo.ugoautomate.dao.DataAccessException;
import edu.liceo.ugoautomate.security.AccessDeniedException;
import edu.liceo.ugoautomate.service.ServiceException;
import edu.liceo.ugoautomate.util.AppConfig;
import edu.liceo.ugoautomate.util.ValidationException;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {
    }

    public static void info(Node owner, String message) {
        show(Views.windowOf(owner), Alert.AlertType.INFORMATION, message);
    }

    public static void error(Node owner, String message) {
        error(Views.windowOf(owner), message);
    }

    public static void error(Window owner, String message) {
        show(owner, Alert.AlertType.ERROR, message);
    }

    public static void error(Node owner, Throwable error) {
        error(owner, userMessage(error));
    }

    public static boolean confirm(Node owner, String message) {
        ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
        Alert alert = alert(Views.windowOf(owner), Alert.AlertType.CONFIRMATION, message);
        alert.getButtonTypes().setAll(yes, no);
        Optional<ButtonType> choice = alert.showAndWait();
        return choice.isPresent() && choice.get() == yes;
    }

    public static String userMessage(Throwable error) {
        if (error instanceof ValidationException || error instanceof ServiceException
                || error instanceof AccessDeniedException) {
            return error.getMessage();
        }
        error.printStackTrace();
        if (error instanceof DataAccessException) {
            return "A database error occurred. Please try again.\n\nDetails: " + error.getMessage();
        }
        return "Something went wrong: " + error;
    }

    private static void show(Window owner, Alert.AlertType type, String message) {
        alert(owner, type, message).showAndWait();
    }

    private static Alert alert(Window owner, Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(AppConfig.APP_NAME);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().getStylesheets().add(Views.stylesheet());
        alert.getDialogPane().setMinWidth(380);
        return alert;
    }
}
