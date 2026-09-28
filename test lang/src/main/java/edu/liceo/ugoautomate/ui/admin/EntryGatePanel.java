package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.QrScanPanel;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.ui.common.WrapLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Gate console for Campus Entry and Access Verification (F-4.1 - F-4.4).
 * Accepts student entry passes and guest visit passes by QR, or student
 * credentials typed at the gate. Only verified entries are logged.
 */
public class EntryGatePanel extends JPanel {

    private final AppContext ctx;
    private final QrScanPanel scanPanel;
    private final JTextField studentNumber = new JTextField(14);
    private final JPasswordField password = new JPasswordField(14);
    private final JButton verifyButton = UiTheme.primaryButton("Verify & Record Entry");

    public EntryGatePanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.scanPanel = new QrScanPanel(
                "Scan a student entry pass or a guest visit pass. Valid entries are recorded automatically.",
                ctx.entries()::verifyQr);

        verifyButton.addActionListener(e -> verifyCredentials());
        password.addActionListener(e -> verifyCredentials());

        JPanel credentials = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 4));
        credentials.add(new JLabel("Student ID"));
        credentials.add(studentNumber);
        credentials.add(new JLabel("Password"));
        credentials.add(password);
        credentials.add(verifyButton);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.add(UiTheme.card("Student without a pass? Verify by account credentials", credentials),
                BorderLayout.NORTH);
        body.add(scanPanel, BorderLayout.CENTER);

        add(UiTheme.page(AdminDashboard.GATE,
                "Verify authorization before approving campus entry. Guests must have a visit registered for today.",
                body), BorderLayout.CENTER);
    }

    private void verifyCredentials() {
        String number = studentNumber.getText();
        char[] pw = password.getPassword();
        verifyButton.setEnabled(false);
        scanPanel.getBanner().showProcessing();
        long started = System.nanoTime();
        Async.run(this, () -> ctx.entries().verifyStudentCredentials(number, pw), result -> {
            scanPanel.showResult(result, (System.nanoTime() - started) / 1_000_000);
            if (result.isSuccess()) {
                studentNumber.setText("");
            }
        }, () -> {
            password.setText("");
            verifyButton.setEnabled(true);
        });
    }
}
