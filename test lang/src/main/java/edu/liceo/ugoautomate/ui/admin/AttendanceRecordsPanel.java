package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.service.AttendanceService;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;
import edu.liceo.ugoautomate.ui.common.WrapLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * Administrator view and management of recorded attendance (F-3.4, F-5.3).
 */
public class AttendanceRecordsPanel extends JPanel implements Refreshable {

    /** Combo item wrapping a session, or "all recent" when {@code session} is null. */
    private record SessionOption(AttendanceSession session) {
        @Override
        public String toString() {
            return session == null
                    ? "All sessions (latest " + AttendanceService.RECENT_LIMIT + " records)"
                    : session.getTitle() + "  (" + DisplayTime.of(session.getStartTime()) + ")";
        }
    }

    private final AppContext ctx;
    private final JComboBox<SessionOption> sessionCombo = new JComboBox<>();
    private final DataTable<AttendanceRecord> table = new DataTable<>(new ListTableModel<>(
            column("Session / Activity", AttendanceRecord::getSessionTitle),
            column("Student ID", AttendanceRecord::getStudentNumber),
            column("Student Name", AttendanceRecord::getStudentName),
            column("Course", AttendanceRecord::getCourse),
            column("Recorded At", DisplayTime.class, r -> DisplayTime.of(r.getRecordedAt()))));
    private Long pendingSessionId;
    private boolean updatingCombo;

    public AttendanceRecordsPanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;

        sessionCombo.setPrototypeDisplayValue(new SessionOption(null));
        sessionCombo.addActionListener(e -> {
            if (!updatingCombo) {
                loadRecords();
            }
        });
        JButton reload = new JButton("Refresh");
        reload.addActionListener(e -> refresh());
        JButton addManual = UiTheme.primaryButton("Add Manual Record");
        addManual.addActionListener(e -> addManualRecord());
        JButton delete = UiTheme.dangerButton("Delete Record");
        delete.addActionListener(e -> deleteRecord());

        JPanel filter = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        filter.add(new JLabel("Session:"));
        filter.add(sessionCombo);
        filter.add(reload);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.add(filter, BorderLayout.NORTH);
        body.add(table.getComponent(), BorderLayout.CENTER);
        body.add(UiTheme.buttonRow(addManual, delete), BorderLayout.SOUTH);
        add(UiTheme.page(AdminDashboard.RECORDS, "Recorded attendance with each student's identity, date, and time.",
                body), BorderLayout.CENTER);
    }

    /** Selects a session in the records panel hosted by the given dashboard. */
    static void selectSession(Container root, long sessionId) {
        for (Component c : root.getComponents()) {
            if (c instanceof AttendanceRecordsPanel panel) {
                panel.pendingSessionId = sessionId;
                panel.refresh();
                return;
            }
            if (c instanceof Container container) {
                selectSession(container, sessionId);
            }
        }
    }

    @Override
    public void refresh() {
        Long keep = pendingSessionId != null ? pendingSessionId : selectedSessionId();
        pendingSessionId = null;
        Async.run(this, ctx.attendance()::listSessions, sessions -> {
            updatingCombo = true;
            sessionCombo.removeAllItems();
            sessionCombo.addItem(new SessionOption(null));
            SessionOption toSelect = null;
            for (AttendanceSession s : sessions) {
                SessionOption option = new SessionOption(s);
                sessionCombo.addItem(option);
                if (keep != null && s.getId() == keep) {
                    toSelect = option;
                }
            }
            if (toSelect != null) {
                sessionCombo.setSelectedItem(toSelect);
            }
            updatingCombo = false;
            loadRecords();
        });
    }

    private Long selectedSessionId() {
        SessionOption option = (SessionOption) sessionCombo.getSelectedItem();
        return option == null || option.session() == null ? null : option.session().getId();
    }

    private void loadRecords() {
        Long sessionId = selectedSessionId();
        Async.run(this, () -> sessionId == null
                ? ctx.attendance().recentRecords()
                : ctx.attendance().recordsForSession(sessionId), (List<AttendanceRecord> rows) -> table.setRows(rows));
    }

    private void addManualRecord() {
        Long sessionId = selectedSessionId();
        if (sessionId == null) {
            Dialogs.info(this, "Choose a specific session in the Session list first.");
            return;
        }
        String number = JOptionPane.showInputDialog(this, "Student ID to record for this session:",
                "Add Manual Record", JOptionPane.PLAIN_MESSAGE);
        if (number == null || number.isBlank()) {
            return;
        }
        Async.run(this, () -> {
            ctx.attendance().addManualRecord(sessionId, number);
            return null;
        }, ignored -> loadRecords());
    }

    private void deleteRecord() {
        Optional<AttendanceRecord> selected = table.getSelected();
        if (selected.isEmpty()) {
            Dialogs.info(this, "Select a record first.");
            return;
        }
        AttendanceRecord r = selected.get();
        if (!Dialogs.confirm(this, "Delete the attendance of " + r.getStudentName() + " for \""
                + r.getSessionTitle() + "\"?")) {
            return;
        }
        Async.run(this, () -> {
            ctx.attendance().deleteRecord(r.getId());
            return null;
        }, ignored -> loadRecords());
    }
}
