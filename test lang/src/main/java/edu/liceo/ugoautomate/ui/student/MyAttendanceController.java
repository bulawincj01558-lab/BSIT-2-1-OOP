package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.Page;
import edu.liceo.ugoautomate.ui.common.Tables;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public class MyAttendanceController implements Page {
    private final AppContext ctx;

    @FXML private TableView<AttendanceRecord> table;
    @FXML private Label count;

    public MyAttendanceController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Tables.setup(table, "No attendance recorded yet. Use Scan Attendance in class.");
        Tables.column(table, "Session", AttendanceRecord::getSessionTitle);
        Tables.column(table, "Date and time recorded", AttendanceRecord::getRecordedAt, DisplayTime::format);
        Tables.column(table, "Scan method", AttendanceRecord::getMethod);
    }

    @Override
    public void onShow() {
        reload();
    }

    @FXML
    private void reload() {
        Async.run(table, () -> ctx.attendance().myAttendance(), records -> {
            Tables.setRows(table, records);
            count.setText(records.size() + " record(s)");
        });
    }
}
