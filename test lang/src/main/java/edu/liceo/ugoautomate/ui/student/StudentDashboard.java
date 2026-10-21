package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.ui.common.DashboardController;
import edu.liceo.ugoautomate.ui.common.DataStructuresController;
import edu.liceo.ugoautomate.ui.common.HomeController;
import edu.liceo.ugoautomate.ui.common.HomeController.Shortcut;
import edu.liceo.ugoautomate.ui.common.ProfileController;
import edu.liceo.ugoautomate.ui.common.QrScanController;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;

public final class StudentDashboard {
    private static final String HOME = "Home";
    private static final String SCAN = "Scan Attendance";
    private static final String HISTORY = "My Attendance";
    private static final String ENTRY = "My Entry QR";
    private static final String PROGRAM = "My Class Program";
    private static final String EVALUATION = "Faculty Evaluation";
    private static final String DISCIPLINE = "My Discipline Record";
    private static final String TUITION = "My Tuition";
    private static final String DATA_STRUCTURES = "Data Structures";
    private static final String PROFILE = "My Profile";

    private StudentDashboard() {
    }

    public static View<DashboardController> create(AppContext ctx, User student) {
        View<DashboardController> view = Views.load(ctx, "common/Dashboard.fxml");
        DashboardController d = view.controller();
        d.setRole("Student");

        d.addPage(HOME, HomeController.create(ctx, "Welcome, " + student.getFirstName() + "!", student.getSummary(), d::showPage,
                new Shortcut(SCAN, "Show your attendance QR to staff, or scan your class or event QR code."),
                new Shortcut(ENTRY, "Show your entry QR code at the campus gate."),
                new Shortcut(PROGRAM, "See your classes this term: schedule, room, and instructor."),
                new Shortcut(HISTORY, "See the classes and events you have attended."),
                new Shortcut(EVALUATION, "Rate your teachers for this term. Your answers are anonymous."),
                new Shortcut(DISCIPLINE, "See any disciplinary cases on your record and their status."),
                new Shortcut(TUITION, "See your tuition balance, payments, and discounts this term."),
                new Shortcut(DATA_STRUCTURES, "See the data structures behind the app, with memory allocation and time complexity."),
                new Shortcut(PROFILE, "View and update your profile or change your password.")));
        View<SubjectPickerController> picker = Views.load(ctx, "student/SubjectPicker.fxml");
        View<QrScanController> scan = QrScanController.create(ctx, "Scan Attendance",
                "Upload or paste your attendance QR below, show it to your instructor, or scan the session QR code they show you.",
                d.scanHistory(), (code, method) -> {
                    ClassOffering subject = picker.controller().selected();
                    return subject == null
                            ? ctx.attendance().recordAttendance(code, method)
                            : ctx.attendance().recordOwnQrForClass(code, method, subject.getId());
                });
        scan.controller().addCompanion(Views.load(ctx, "student/AttendanceQr.fxml"));
        scan.controller().addCompanion(picker);
        d.addPage(SCAN, scan);
        d.addPage(PROGRAM, Views.load(ctx, "student/ClassProgram.fxml"));
        d.addPage(HISTORY, Views.load(ctx, "student/MyAttendance.fxml"));
        View<EntryQrController> entry = Views.load(ctx, "student/EntryQr.fxml");
        entry.controller().setOnEditProfile(() -> d.showPage(PROFILE));
        d.addPage(ENTRY, entry);
        d.addPage(EVALUATION, Views.load(ctx, "student/FacultyEvaluation.fxml"));
        d.addPage(DISCIPLINE, Views.load(ctx, "student/MyDiscipline.fxml"));
        d.addPage(TUITION, Views.load(ctx, "student/MyTuition.fxml"));
        d.addPage(DATA_STRUCTURES, DataStructuresController.create(ctx, d));
        View<ProfileController> profile = Views.load(ctx, "common/Profile.fxml");
        profile.controller().setOnSaved(d::refreshHeader);
        d.addPage(PROFILE, profile);
        d.showFirstPage();
        return view;
    }
}
