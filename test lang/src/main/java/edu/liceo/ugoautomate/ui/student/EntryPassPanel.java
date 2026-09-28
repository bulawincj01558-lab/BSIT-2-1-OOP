package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.service.EntryService;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.FormBuilder;
import edu.liceo.ugoautomate.ui.common.QrDisplayDialog;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.ResponsiveGridLayout;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

/**
 * Displays the student's short-lived, signed campus entry pass (F-4.1). The
 * pass renews itself automatically when it expires while visible.
 */
public class EntryPassPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final JPanel qrHolder = new JPanel(new BorderLayout());
    private final JLabel countdown = new JLabel(" ");
    private final Timer timer = new Timer(1000, e -> tick());
    private Instant expiresAt;
    private boolean loading;

    public EntryPassPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        Student student = (Student) ctx.session().currentUser().orElseThrow();

        countdown.setFont(countdown.getFont().deriveFont(Font.BOLD, 16f));
        countdown.setForeground(UiTheme.PRIMARY_DARK);
        JButton renew = UiTheme.primaryButton("Refresh Pass");
        renew.addActionListener(e -> refresh());

        JLabel name = new JLabel(student.getFullName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        JPanel info = new FormBuilder()
                .addFull(name)
                .add("Student ID", new JLabel(student.getStudentNumber()))
                .add("Course / Year", new JLabel(student.getCourseAndYear()))
                .addFull(countdown)
                .addFull(UiTheme.subtitle("<html>Show this QR code to the gate staff. For security the pass is valid for "
                        + EntryService.ENTRY_PASS_VALIDITY.toMinutes()
                        + " minutes and renews automatically.<br><br>No phone or screen? The gate staff can verify "
                        + "you with your student ID and password instead.</html>"))
                .addFull(UiTheme.buttonRow(renew))
                .build();

        qrHolder.add(new JLabel("Generating pass...", SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel columns = new JPanel(new ResponsiveGridLayout(340, 2, 20, 20));
        columns.add(UiTheme.card("Your Entry Pass", qrHolder));
        columns.add(UiTheme.card("Pass Details", info));
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(columns, BorderLayout.NORTH);

        add(UiTheme.page(StudentDashboard.ENTRY_PASS, "Present this pass for campus entry verification.", holder),
                BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        Async.run(this, ctx.entries()::myEntryPass, pass -> {
            expiresAt = pass.expiresAt();
            Student student = (Student) ctx.session().currentUser().orElseThrow();
            qrHolder.removeAll();
            qrHolder.add(QrDisplayDialog.qrPanel(this, pass.payload(),
                    "entry-pass-" + student.getStudentNumber(), 260), BorderLayout.CENTER);
            qrHolder.revalidate();
            qrHolder.repaint();
            tick();
            timer.start();
        }, () -> loading = false);
    }

    private void tick() {
        if (expiresAt == null) {
            return;
        }
        if (!isShowing()) {
            timer.stop();
            return;
        }
        long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (seconds <= 0) {
            countdown.setText("Pass expired - renewing...");
            refresh();
        } else {
            countdown.setText(String.format("Valid for %d:%02d", seconds / 60, seconds % 60));
        }
    }
}
