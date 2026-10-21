package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.QrView;
import edu.liceo.ugoautomate.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AttendanceQrController implements Page {
    private final AppContext ctx;

    @FXML private Label help;
    @FXML private QrView qrView;

    public AttendanceQrController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        help.setText("Save or copy this code, then upload or paste it below to be marked present in the selected subject. "
                + "You can also show it to your instructor. It is valid for "
                + AppConfig.STUDENT_ATTENDANCE_QR_VALIDITY.toMinutes() + " minutes, then a new one appears automatically.");
        qrView.setOnExpired(this::generate);
    }

    @Override
    public void onShow() {
        generate();
    }

    @FXML
    private void generate() {
        Async.run(qrView, () -> ctx.attendance().issueStudentAttendanceQr(), token -> {
            String name = ctx.session().current().map(User::getFullName).orElse("Student");
            qrView.show(token, "Attendance QR - " + name, "attendance-qr.png");
        });
    }

    @Override
    public void onHide() {
        qrView.clear("No QR code generated yet.");
    }
}
