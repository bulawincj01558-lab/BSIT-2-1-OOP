package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.QrDisplayDialog;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Attendance session management and QR code generation (F-3.1, F-5.3).
 */
public class AttendanceSessionsPanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final AdminDashboard dashboard;
    private final DataTable<AttendanceSession> table = new DataTable<>(new ListTableModel<>(
            column("Title", AttendanceSession::getTitle),
            column("Type", AttendanceSession::getSessionType),
            column("Venue", AttendanceSession::getVenue),
            column("Start", DisplayTime.class, s -> DisplayTime.of(s.getStartTime())),
            column("End", DisplayTime.class, s -> DisplayTime.of(s.getEndTime())),
            column("Status", s -> s.isActive() ? "Open" : "Closed"),
            column("Attendees", Integer.class, AttendanceSession::getAttendeeCount)));

    public AttendanceSessionsPanel(AppContext ctx, AdminDashboard dashboard) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.dashboard = dashboard;

        JButton create = UiTheme.primaryButton("New Session");
        create.addActionListener(e -> {
            if (SessionEditDialog.open(this, ctx, null)) {
                refresh();
            }
        });
        JButton edit = new JButton("Edit");
        edit.addActionListener(e -> selected().ifPresent(s -> {
            if (SessionEditDialog.open(this, ctx, s)) {
                refresh();
            }
        }));
        JButton showQr = UiTheme.primaryButton("Show QR Code");
        showQr.addActionListener(e -> selected().ifPresent(this::showQr));
        table.onDoubleClick(() -> table.getSelected().ifPresent(this::showQr));
        JButton regenerate = new JButton("Regenerate QR");
        regenerate.addActionListener(e -> selected().ifPresent(this::regenerate));
        JButton toggle = new JButton("Open / Close");
        toggle.addActionListener(e -> selected().ifPresent(this::toggle));
        JButton records = new JButton("View Records");
        records.addActionListener(e -> selected().ifPresent(s -> dashboard.showRecordsFor(s.getId())));
        JButton delete = UiTheme.dangerButton("Delete");
        delete.addActionListener(e -> selected().ifPresent(this::delete));

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(table.getComponent(), BorderLayout.CENTER);
        body.add(UiTheme.buttonRow(create, edit, showQr, regenerate, toggle, records, delete), BorderLayout.SOUTH);
        add(UiTheme.page(AdminDashboard.SESSIONS,
                "Create authorized class or event sessions and display their QR code for students to scan.",
                body), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.attendance()::listSessions, table::setRows);
    }

    private Optional<AttendanceSession> selected() {
        Optional<AttendanceSession> s = table.getSelected();
        if (s.isEmpty()) {
            Dialogs.info(this, "Select a session first.");
        }
        return s;
    }

    private void showQr(AttendanceSession s) {
        Async.run(this, () -> ctx.attendance().sessionQrPayload(s.getId()), payload ->
                QrDisplayDialog.show(this, s.getTitle(),
                        s.getSessionType().getDisplayName() + (s.getVenue() == null ? "" : " at " + s.getVenue())
                                + "\n" + DateTimeUtil.format(s.getStartTime()) + " - "
                                + DateTimeUtil.formatTime(s.getEndTime())
                                + "\nCheck-in opens " + AttendanceSession.EARLY_CHECK_IN_MINUTES
                                + " minutes before the start.",
                        payload, "attendance-" + s.getId()));
    }

    private void regenerate(AttendanceSession s) {
        if (!Dialogs.confirm(this, "Generate a new QR code for \"" + s.getTitle() + "\"?\n\n"
                + "Previously displayed, printed, or shared codes will stop working immediately.")) {
            return;
        }
        Async.run(this, () -> {
            ctx.attendance().regenerateQr(s.getId());
            return null;
        }, ignored -> showQr(s));
    }

    private void toggle(AttendanceSession s) {
        Async.run(this, () -> {
            ctx.attendance().setSessionActive(s.getId(), !s.isActive());
            return null;
        }, ignored -> refresh());
    }

    private void delete(AttendanceSession s) {
        if (!Dialogs.confirm(this, "Delete \"" + s.getTitle() + "\" and its " + s.getAttendeeCount()
                + " attendance record(s)? This cannot be undone.")) {
            return;
        }
        Async.run(this, () -> {
            ctx.attendance().deleteSession(s.getId());
            return null;
        }, ignored -> refresh());
    }
}
