package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.QrView;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class EntryQrController implements Page {
    private final AppContext ctx;
    private Runnable onEditProfile = () -> { };

    @FXML private Label help;
    @FXML private VBox profileCard;
    @FXML private Label profileName;
    @FXML private Label profileSummary;
    @FXML private QrView qrView;

    public EntryQrController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        help.setText("Show this code to the gate staff. For security it is valid for "
                + AppConfig.STUDENT_ENTRY_QR_VALIDITY.toMinutes() + " minutes only, then a new one appears automatically.");
        qrView.setOnExpired(this::generate);
    }

    public void setOnEditProfile(Runnable onEditProfile) {
        this.onEditProfile = onEditProfile;
    }

    @Override
    public void onShow() {
        Async.run(profileCard, () -> ctx.profiles().myProfile(), user -> {
            profileName.setText(user.getFullName());
            profileSummary.setText(user.getSummary());
        });
        generate();
    }

    @FXML
    private void editProfile() {
        onEditProfile.run();
    }

    @FXML
    private void generate() {
        Async.run(qrView, () -> ctx.entries().issueStudentEntryQr(), token -> {
            String name = ctx.session().current().map(User::getFullName).orElse("Student");
            qrView.show(token, "Entry QR - " + name, "entry-qr.png");
        });
    }

    @Override
    public void onHide() {
        qrView.clear("No QR code generated yet.");
    }
}
