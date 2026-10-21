package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSlot;
import edu.liceo.ugoautomate.model.VerificationMethod;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private static final String TIMES = "SELECT session_id, student_id, slot, recorded_at FROM attendance_time_logs";

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
            return withTimes(queryList(SELECT + " ORDER BY r.recorded_at DESC LIMIT 2000", JdbcAttendanceRecordDao::map),
                    queryList(TIMES, JdbcAttendanceRecordDao::mapTime));
        }
        return withTimes(queryList(SELECT + " WHERE r.session_id = ? ORDER BY r.recorded_at DESC",
                        JdbcAttendanceRecordDao::map, sessionId),
                queryList(TIMES + " WHERE session_id = ?", JdbcAttendanceRecordDao::mapTime, sessionId));
    }

    @Override
    public List<AttendanceRecord> findByStudent(long studentId) {
        return withTimes(queryList(SELECT + " WHERE r.student_id = ? ORDER BY r.recorded_at DESC",
                        JdbcAttendanceRecordDao::map, studentId),
                queryList(TIMES + " WHERE student_id = ?", JdbcAttendanceRecordDao::mapTime, studentId));
    }

    @Override
    public void delete(long id) {
        inTransaction(connection -> {
            update(connection, """
                    DELETE FROM attendance_time_logs WHERE EXISTS (
                        SELECT 1 FROM attendance_records r
                        WHERE r.id = ? AND r.session_id = attendance_time_logs.session_id
                          AND r.student_id = attendance_time_logs.student_id)
                    """, id);
            return update(connection, "DELETE FROM attendance_records WHERE id = ?", id);
        });
    }

    @Override
    public Optional<LocalDateTime> findTime(long sessionId, long studentId, AttendanceSlot slot) {
        return queryOne("SELECT recorded_at FROM attendance_time_logs WHERE session_id = ? AND student_id = ? AND slot = ?",
                rs -> getDateTime(rs, "recorded_at"), sessionId, studentId, slot);
    }

    @Override
    public void createTime(long sessionId, long studentId, AttendanceSlot slot, LocalDateTime recordedAt,
                           VerificationMethod method) {
        insert("INSERT INTO attendance_time_logs (session_id, student_id, slot, recorded_at, method) VALUES (?, ?, ?, ?, ?)",
                sessionId, studentId, slot, recordedAt, method);
    }

    private record TimeLog(long sessionId, long studentId, AttendanceSlot slot, LocalDateTime recordedAt) {
    }

    private static List<AttendanceRecord> withTimes(List<AttendanceRecord> records, List<TimeLog> logs) {
        Map<String, AttendanceRecord> byKey = new HashMap<>();
        records.forEach(r -> byKey.put(r.getSessionId() + ":" + r.getStudentId(), r));
        for (TimeLog log : logs) {
            AttendanceRecord r = byKey.get(log.sessionId() + ":" + log.studentId());
            if (r != null) {
                r.setTime(log.slot(), log.recordedAt());
            }
        }
        return records;
    }

    private static TimeLog mapTime(ResultSet rs) throws SQLException {
        return new TimeLog(rs.getLong("session_id"), rs.getLong("student_id"),
                AttendanceSlot.valueOf(rs.getString("slot")), getDateTime(rs, "recorded_at"));
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
