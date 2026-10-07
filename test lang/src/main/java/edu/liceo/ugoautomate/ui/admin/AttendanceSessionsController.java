package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.QrDialogController;
import edu.liceo.ugoautomate.ui.common.Tables;
import edu.liceo.ugoautomate.ui.common.Views;
import edu.liceo.ugoautomate.ui.common.Views.View;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public class AttendanceSessionsController implements Page {
    private static final String SELECT_HINT = "Select a session in the list first.";

    private final AppContext ctx;
    private Consumer<AttendanceSession> onViewRecords = session -> { };

    @FXML private TableView<AttendanceSession> table;

    public AttendanceSessionsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No sessions yet. Press \"New session\" to create one.");
        Tables.column(table, "Title", AttendanceSession::getTitle);
        Tables.column(table, "Type", AttendanceSession::getType);
        Tables.column(table, "Location", AttendanceSession::getLocationName);
        Tables.column(table, "Starts", AttendanceSession::getStartsAt, DisplayTime::format);
        Tables.column(table, "Ends", AttendanceSession::getEndsAt, DisplayTime::format);
        Tables.column(table, "Status", AttendanceSessionsController::status);
        Tables.column(table, "Attendees", AttendanceSession::getAttendeeCount);
        Tables.onDoubleClick(table, this::showQr);
    }

    public void setOnViewRecords(Consumer<AttendanceSession> onViewRecords) {
        this.onViewRecords = onViewRecords;
    }

    private static String status(AttendanceSession s) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(s.getStartsAt())) {
            return "Upcoming";
        }
        return s.isOpenAt(now) ? "Open now" : "Ended";
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> ctx.attendance().listSessions(), rows -> Tables.setRows(table, rows));
    }

    @FXML
    private void create() {
        openEditor(null);
    }

    @FXML
    private void edit() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::openEditor);
    }

    private void openEditor(AttendanceSession session) {
        View<SessionEditController> view = Views.load(ctx, "admin/SessionEdit.fxml");
        view.controller().setSession(session);
        Views.showDialog(table, session == null ? "New Attendance Session" : "Edit Attendance Session", view.root());
        if (view.controller().isSaved()) {
            reload();
        }
    }

    @FXML
    private void showQr() {
        Tables.selected(table, SELECT_HINT).ifPresent(this::showQr);
    }

    private void showQr(AttendanceSession session) {
        Async.run(table, () -> ctx.attendance().issueSessionQr(session.getId()), token ->
                QrDialogController.show(ctx, table, "Attendance QR Code", token,
                        session.getTitle(), "attendance-session-" + session.getId() + ".png"));
    }

    @FXML
    private void viewRecords() {
        Tables.selected(table, SELECT_HINT).ifPresent(onViewRecords);
    }

    @FXML
    private void delete() {
        Tables.selected(table, SELECT_HINT).ifPresent(session -> {
            if (!Dialogs.confirm(table, "Delete \"" + session.getTitle() + "\" and its "
                    + session.getAttendeeCount() + " attendance record(s)?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.attendance().deleteSession(session.getId());
                return Boolean.TRUE;
            }, ok -> reload());
        });
    }
}
