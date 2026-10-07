package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.VerificationMethod;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcAttendanceRecordDao extends AbstractJdbcDao implements AttendanceRecordDao {
    private static final String SELECT = """
            SELECT r.id, r.session_id, r.student_id, r.recorded_at, r.method,
                   s.title AS session_title,
                   u.first_name || ' ' || u.last_name AS student_name,
                   st.student_number
            FROM attendance_records r
            JOIN attendance_sessions s ON s.id = r.session_id
            JOIN users u ON u.id = r.student_id
            LEFT JOIN students st ON st.user_id = r.student_id
            """;

    public JdbcAttendanceRecordDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long create(AttendanceRecord r) {
        long id = insert("INSERT INTO attendance_records (session_id, student_id, recorded_at, method) VALUES (?, ?, ?, ?)",
                r.getSessionId(), r.getStudentId(), r.getRecordedAt(), r.getMethod());
        r.setId(id);
        return id;
    }

    @Override
    public boolean exists(long sessionId, long studentId) {
        return queryExists("SELECT 1 FROM attendance_records WHERE session_id = ? AND student_id = ?", sessionId, studentId);
    }

    @Override
    public List<AttendanceRecord> findAll(Long sessionId) {
        if (sessionId == null) {
            return queryList(SELECT + " ORDER BY r.recorded_at DESC LIMIT 2000", JdbcAttendanceRecordDao::map);
        }
        return queryList(SELECT + " WHERE r.session_id = ? ORDER BY r.recorded_at DESC", JdbcAttendanceRecordDao::map, sessionId);
    }

    @Override
    public List<AttendanceRecord> findByStudent(long studentId) {
        return queryList(SELECT + " WHERE r.student_id = ? ORDER BY r.recorded_at DESC", JdbcAttendanceRecordDao::map, studentId);
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM attendance_records WHERE id = ?", id);
    }

    private static AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord r = new AttendanceRecord();
        r.setId(rs.getLong("id"));
        r.setSessionId(rs.getLong("session_id"));
        r.setStudentId(rs.getLong("student_id"));
        r.setRecordedAt(getDateTime(rs, "recorded_at"));
        r.setMethod(VerificationMethod.valueOf(rs.getString("method")));
        r.setSessionTitle(rs.getString("session_title"));
        r.setStudentName(rs.getString("student_name"));
        r.setStudentNumber(rs.getString("student_number"));
        return r;
    }
}
