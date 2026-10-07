package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.QrView;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class EntryQrController implements Page {
    private final AppContext ctx;

    @FXML private Label help;
    @FXML private QrView qrView;

    public EntryQrController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        help.setText("Generate a code and show it to the gate staff. For security it is valid for "
                + AppConfig.STUDENT_ENTRY_QR_VALIDITY.toMinutes() + " minutes only.");
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
