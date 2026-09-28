package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SQLite implementation of {@link AttendanceRecordDao}. Duplicate check-ins are
 * rejected by the UNIQUE(session_id, student_user_id) constraint via
 * {@code INSERT OR IGNORE}, which is race-free across concurrent scans.
 */
public class JdbcAttendanceRecordDao extends AbstractJdbcDao implements AttendanceRecordDao {

    private static final String SELECT = """
            SELECT r.id, r.session_id, r.student_user_id, r.recorded_at,
                   a.title AS session_title, st.student_number, u.full_name, st.course
            FROM attendance_records r
            JOIN attendance_sessions a ON a.id = r.session_id
            JOIN users u ON u.id = r.student_user_id
            LEFT JOIN students st ON st.user_id = r.student_user_id
            """;

    public JdbcAttendanceRecordDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean insertIfAbsent(long sessionId, long studentUserId, LocalDateTime recordedAt) {
        return update("""
                INSERT OR IGNORE INTO attendance_records (session_id, student_user_id, recorded_at)
                VALUES (?, ?, ?)
                """, ps -> {
            ps.setLong(1, sessionId);
            ps.setLong(2, studentUserId);
            ps.setString(3, DateTimeUtil.toDb(recordedAt));
        }) == 1;
    }

    @Override
    public List<AttendanceRecord> findBySession(long sessionId) {
        return query(SELECT + " WHERE r.session_id = ? ORDER BY r.recorded_at",
                ps -> ps.setLong(1, sessionId), JdbcAttendanceRecordDao::map);
    }

    @Override
    public List<AttendanceRecord> findByStudent(long studentUserId) {
        return query(SELECT + " WHERE r.student_user_id = ? ORDER BY r.recorded_at DESC",
                ps -> ps.setLong(1, studentUserId), JdbcAttendanceRecordDao::map);
    }

    @Override
    public List<AttendanceRecord> findRecent(int limit) {
        return query(SELECT + " ORDER BY r.recorded_at DESC LIMIT ?",
                ps -> ps.setInt(1, limit), JdbcAttendanceRecordDao::map);
    }

    @Override
    public boolean delete(long recordId) {
        return update("DELETE FROM attendance_records WHERE id = ?", ps -> ps.setLong(1, recordId)) > 0;
    }

    private static AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord r = new AttendanceRecord();
        r.setId(rs.getLong("id"));
        r.setSessionId(rs.getLong("session_id"));
        r.setStudentUserId(rs.getLong("student_user_id"));
        r.setRecordedAt(getDateTime(rs, "recorded_at"));
        r.setSessionTitle(rs.getString("session_title"));
        r.setStudentNumber(rs.getString("student_number"));
        r.setStudentName(rs.getString("full_name"));
        r.setCourse(rs.getString("course"));
        return r;
    }
}
