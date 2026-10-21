package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.DashboardController;
import edu.liceo.ugoautomate.ui.common.DataStructuresController;
import edu.liceo.ugoautomate.ui.common.HomeController;
import edu.liceo.ugoautomate.ui.common.HomeController.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfileController;
import edu.liceo.ugoautomate.ui.common.QrScanController;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;

public final class AdminDashboard {
    private static final String HOME = "Home";
    private static final String GATE = "Entry Gate";
    private static final String ACCOUNTS = "Accounts";
    private static final String SESSIONS = "Attendance Sessions";
    private static final String LOGS = "Entry & Visit Logs";
    private static final String CLASSES = "Class Programs";
    private static final String DISCIPLINE = "Discipline";
    private static final String TUITION = "Tuition";
    private static final String FACULTY = "Faculty";
    private static final String RESULTS = "Evaluation Results";
    private static final String DATA_STRUCTURES = "Data Structures";
    private static final String PROFILE = "My Profile";

    private AdminDashboard() {
    }

    public static View<DashboardController> create(AppContext ctx, User admin) {
        View<DashboardController> view = Views.load(ctx, "common/Dashboard.fxml");
        DashboardController d = view.controller();
        d.setRole("Administrator");

        d.addPage(HOME, HomeController.create(ctx, "Welcome, " + admin.getFirstName() + "!", admin.getSummary(), d::showPage,
                new Shortcut(GATE, "Verify student entry QR codes and guest passes, and log campus entries."),
                new Shortcut(ACCOUNTS, "View, edit, deactivate, or remove student and guest accounts."),
                new Shortcut(SESSIONS, "Create class/event sessions and display their attendance QR codes."),
                new Shortcut(LOGS, "Review campus entry logs and guest visits."),
                new Shortcut(CLASSES, "Create each term's classes and enroll students without schedule conflicts."),
                new Shortcut(FACULTY, "Add and update the teachers that students evaluate."),
                new Shortcut(RESULTS, "Open or close faculty evaluation and see anonymous results."),
                new Shortcut(DISCIPLINE, "Record, follow up, and resolve students' disciplinary cases."),
                new Shortcut(TUITION, "Assess tuition from class programs and record payments and discounts."),
                new Shortcut(DATA_STRUCTURES, "See the data structures behind the app, with memory allocation and time complexity.")));
        d.addPage(GATE, QrScanController.create(ctx, "Entry Gate Verification",
                "Scan a student's entry QR or a guest pass. Entries are logged only when verification succeeds.",
                d.scanHistory(), (code, method) -> ctx.entries().verifyEntry(code, method)));
        d.addPage(ACCOUNTS, Views.load(ctx, "admin/Accounts.fxml"));
        d.addPage(SESSIONS, Views.load(ctx, "admin/AttendanceSessions.fxml"));
        d.addPage(LOGS, Views.load(ctx, "admin/EntryLogs.fxml"));
        d.addPage(CLASSES, Views.load(ctx, "admin/ClassPrograms.fxml"));
        d.addPage(FACULTY, Views.load(ctx, "admin/Faculty.fxml"));
        d.addPage(RESULTS, Views.load(ctx, "admin/EvaluationResults.fxml"));
        d.addPage(DISCIPLINE, Views.load(ctx, "admin/Discipline.fxml"));
        d.addPage(TUITION, Views.load(ctx, "admin/Tuition.fxml"));
        d.addPage(DATA_STRUCTURES, DataStructuresController.create(ctx, d));
        View<ProfileController> profile = Views.load(ctx, "common/Profile.fxml");
        profile.controller().setOnSaved(d::refreshHeader);
        d.addPage(PROFILE, profile);
        d.showFirstPage();
        return view;
    }
}
