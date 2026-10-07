package edu.liceo.ugoautomate.ui;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.service.GuestRegistration;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Views;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

public class GuestRegistrationController {
    private final AppContext ctx;
    private Consumer<String> onRegistered = email -> { };

    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField contact;
    @FXML private TextField email;
    @FXML private TextField purpose;
    @FXML private TextField personToVisit;
    @FXML private PasswordField password;
    @FXML private PasswordField confirm;
    @FXML private Button registerButton;

    public GuestRegistrationController(AppContext ctx) {
        this.ctx = ctx;
    }

    public void setOnRegistered(Consumer<String> onRegistered) {
        this.onRegistered = onRegistered;
    }

    @FXML
    private void register() {
        GuestRegistration form = new GuestRegistration(
                firstName.getText(), lastName.getText(), contact.getText(), email.getText(),
                purpose.getText(), personToVisit.getText(), password.getText(), confirm.getText());
        registerButton.setDisable(true);
        Async.run(registerButton, () -> ctx.auth().registerGuest(form), guest -> {
            Views.close(registerButton);
            onRegistered.accept(guest.getEmail());
        }, () -> registerButton.setDisable(false));
    }

    @FXML
    private void cancel() {
        Views.close(registerButton);
    }
}
