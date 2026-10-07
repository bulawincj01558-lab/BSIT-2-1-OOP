package edu.liceo.ugoautomate.ui.admin;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.Choice;
import edu.liceo.ugoautomate.ui.common.Dialogs;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;

public class AttendanceRecordsController implements Page {
    private final AppContext ctx;
    private Long pendingSessionId;
    private boolean loadingFilter;

    @FXML private ComboBox<Choice<Long>> sessionFilter;
    @FXML private Label count;
    @FXML private TableView<AttendanceRecord> table;

    public AttendanceRecordsController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No attendance recorded for this selection.");
        Tables.column(table, "Student ID", AttendanceRecord::getStudentNumber);
        Tables.column(table, "Student", AttendanceRecord::getStudentName);
        Tables.column(table, "Session", AttendanceRecord::getSessionTitle);
        Tables.column(table, "Date and time recorded", AttendanceRecord::getRecordedAt, DisplayTime::format);
        Tables.column(table, "Scan method", AttendanceRecord::getMethod);
    }

    public void filterBySession(long sessionId) {
        this.pendingSessionId = sessionId;
    }

    @Override
    public void onShow() {
        refresh();
    }

    @FXML
    private void refresh() {
        Long keepId = pendingSessionId != null ? pendingSessionId : Choice.selectedValue(sessionFilter);
        pendingSessionId = null;
        Async.run(table, () -> ctx.attendance().listSessions(), sessions -> {
            fillFilter(sessions, keepId);
            loadRecords();
        });
    }

    private void fillFilter(List<AttendanceSession> sessions, Long selectId) {
        loadingFilter = true;
        sessionFilter.getItems().setAll(new Choice<>("All sessions", null));
        for (AttendanceSession s : sessions) {
            sessionFilter.getItems().add(new Choice<>(s.toString(), s.getId()));
        }
        sessionFilter.getSelectionModel().selectFirst();
        Choice.select(sessionFilter, selectId);
        loadingFilter = false;
    }

    @FXML
    private void filterChanged() {
        if (!loadingFilter) {
            loadRecords();
        }
    }

    private void loadRecords() {
        Long sessionId = Choice.selectedValue(sessionFilter);
        Async.run(table, () -> ctx.attendance().listRecords(sessionId), records -> {
            Tables.setRows(table, records);
            count.setText(records.size() + " record(s)");
        });
    }

    @FXML
    private void deleteSelected() {
        Tables.selected(table, "Select a record in the list first.").ifPresent(record -> {
            if (!Dialogs.confirm(table, "Delete the attendance of " + record.getStudentName()
                    + " for \"" + record.getSessionTitle() + "\"?")) {
                return;
            }
            Async.run(table, () -> {
                ctx.attendance().deleteRecord(record.getId());
                return Boolean.TRUE;
            }, ok -> loadRecords());
        });
    }
}
