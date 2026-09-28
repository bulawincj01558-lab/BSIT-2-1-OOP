package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.ui.common.CampusNavigatorPanel;
import edu.liceo.ugoautomate.ui.common.DashboardFrame;
import edu.liceo.ugoautomate.ui.common.DataStructuresPanel;
import edu.liceo.ugoautomate.ui.common.HomePanel;
import edu.liceo.ugoautomate.ui.common.HomePanel.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfilePanel;

/**
 * Administrator dashboard: entry verification, account, location, and
 * attendance management, and entry/visit logs.
 */
public class AdminDashboard extends DashboardFrame {

    static final String HOME = "Home";
    static final String GATE = "Entry Verification";
    static final String ACCOUNTS = "Accounts";
    static final String LOCATIONS = "Campus Locations";
    static final String SESSIONS = "Attendance Sessions";
    static final String RECORDS = "Attendance Records";
    static final String LOGS = "Entry & Visit Logs";
    static final String NAVIGATOR = "Campus Navigator";
    static final String PROFILE = "My Profile";
    static final String DATA_STRUCTURES = "Data Structures";

    public AdminDashboard(AppContext ctx) {
        super(ctx, "Administrator");
        addPage(HOME, new HomePanel("Administrator Console",
                "Manage campus records and verify entries.", this::showPage,
                new Shortcut(GATE, "Verify student entry passes, guest visit passes, or student credentials at the gate."),
                new Shortcut(SESSIONS, "Create class or event sessions and display their attendance QR codes."),
                new Shortcut(RECORDS, "View, add, or remove recorded attendance per session."),
                new Shortcut(ACCOUNTS, "View and manage student and guest accounts."),
                new Shortcut(LOCATIONS, "Add, update, or remove campus buildings, offices, and facilities."),
                new Shortcut(LOGS, "Review campus entry logs and guest visit registrations."),
                new Shortcut(DATA_STRUCTURES, "See the arrays, lists, stacks, and queues used in this app, with memory use and Big-O time complexity.")));
        addPage(GATE, new EntryGatePanel(ctx));
        addPage(SESSIONS, new AttendanceSessionsPanel(ctx, this));
        addPage(RECORDS, new AttendanceRecordsPanel(ctx));
        addPage(ACCOUNTS, new AccountsPanel(ctx));
        addPage(LOCATIONS, new LocationsPanel(ctx));
        addPage(LOGS, new EntryLogsPanel(ctx));
        addPage(NAVIGATOR, new CampusNavigatorPanel(ctx));
        addPage(PROFILE, new ProfilePanel(ctx));
        addPage(DATA_STRUCTURES, new DataStructuresPanel());
        showPage(HOME);
    }

    /** Opens the Attendance Records page focused on one session. */
    void showRecordsFor(long sessionId) {
        showPage(RECORDS);
        AttendanceRecordsPanel.selectSession(this, sessionId);
    }
}
