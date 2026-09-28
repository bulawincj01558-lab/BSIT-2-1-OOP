package edu.liceo.ugoautomate.ui.student;

import edu.liceo.ugoautomate.AppContext;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.ui.common.Async;
import edu.liceo.ugoautomate.ui.common.DataTable;
import edu.liceo.ugoautomate.ui.common.DisplayTime;
import edu.liceo.ugoautomate.ui.common.ListTableModel;
import edu.liceo.ugoautomate.ui.common.Refreshable;
import edu.liceo.ugoautomate.ui.common.UiTheme;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static edu.liceo.ugoautomate.ui.common.ListTableModel.column;

/**
 * The signed-in student's own attendance history. Students can only see
 * their own records (enforced by the service).
 */
public class MyAttendancePanel extends JPanel implements Refreshable {

    private final AppContext ctx;
    private final DataTable<AttendanceRecord> table = new DataTable<>(new ListTableModel<>(
            column("Session / Activity", AttendanceRecord::getSessionTitle),
            column("Recorded At", DisplayTime.class, r -> DisplayTime.of(r.getRecordedAt()))));

    public MyAttendancePanel(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        add(UiTheme.page(StudentDashboard.HISTORY, "Attendance recorded under your student account.",
                table.getComponent()), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, ctx.attendance()::myAttendance, table::setRows);
    }
}
