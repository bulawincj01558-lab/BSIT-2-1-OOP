package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.CampusNavigatorPanel;
import edu.liceo.ugoautomate.ui.common.DashboardFrame;
import edu.liceo.ugoautomate.ui.common.DataStructuresPanel;
import edu.liceo.ugoautomate.ui.common.HomePanel;
import edu.liceo.ugoautomate.ui.common.HomePanel.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfilePanel;
import edu.liceo.ugoautomate.ui.common.QrScanPanel;
import edu.liceo.ugoautomate.ui.common.UiTheme;

/**
 * Student dashboard: navigator, attendance scanning and history, campus
 * entry pass, and profile.
 */
public class StudentDashboard extends DashboardFrame {

    static final String HOME = "Home";
    static final String NAVIGATOR = "Campus Navigator";
    static final String SCAN = "Scan Attendance";
    static final String HISTORY = "My Attendance";
    static final String ENTRY_PASS = "Entry Pass";
    static final String PROFILE = "My Profile";
    static final String DATA_STRUCTURES = "Data Structures";

    public StudentDashboard(AppContext ctx) {
        super(ctx, "Student");
        String firstName = ctx.session().currentUser().map(User::getFullName).orElse("").split(" ")[0];

        addPage(HOME, new HomePanel("Welcome, " + firstName + "!",
                "What would you like to do today?", this::showPage,
                new Shortcut(SCAN, "Scan the QR code shown by your instructor or event organizer to record your attendance."),
                new Shortcut(ENTRY_PASS, "Show your personal QR entry pass at the campus gate."),
                new Shortcut(NAVIGATOR, "Search the campus directory of buildings, offices, and facilities."),
                new Shortcut(HISTORY, "Review the classes and events where your attendance was recorded."),
                new Shortcut(PROFILE, "View your account and update your contact details or password."),
                new Shortcut(DATA_STRUCTURES, "See the arrays, lists, stacks, and queues used in this app, with memory use and Big-O time complexity.")));
        addPage(NAVIGATOR, new CampusNavigatorPanel(ctx));
        addPage(SCAN, UiTheme.page(SCAN,
                "Scan the authorized QR code for your class or event. Your student ID, date, and time are recorded.",
                new QrScanPanel("Scan, upload, or type the attendance code to record your attendance.",
                        ctx.attendance()::recordAttendance)));
        addPage(HISTORY, new MyAttendancePanel(ctx));
        addPage(ENTRY_PASS, new EntryPassPanel(ctx));
        addPage(PROFILE, new ProfilePanel(ctx));
        addPage(DATA_STRUCTURES, new DataStructuresPanel());
        showPage(HOME);
    }
}
