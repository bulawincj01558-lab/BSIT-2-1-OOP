package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link AttendanceSessionDao}. The attendee count is
 * computed with a correlated sub-query that is served by the
 * (session_id, student_user_id) unique index.
 */
public class JdbcAttendanceSessionDao extends AbstractJdbcDao implements AttendanceSessionDao {

    private static final String SELECT = """
            SELECT s.id, s.title, s.session_type, s.venue, s.start_time, s.end_time, s.qr_nonce, s.active,
                   s.created_by, s.created_at,
                   (SELECT COUNT(*) FROM attendance_records r WHERE r.session_id = s.id) AS attendee_count
            FROM attendance_sessions s
            """;

    public JdbcAttendanceSessionDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long insert(AttendanceSession s) {
        long id = insert("""
                INSERT INTO attendance_sessions
                    (title, session_type, venue, start_time, end_time, qr_nonce, active, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getSessionType().name());
            ps.setString(3, s.getVenue());
            ps.setString(4, DateTimeUtil.toDb(s.getStartTime()));
            ps.setString(5, DateTimeUtil.toDb(s.getEndTime()));
            ps.setString(6, s.getQrNonce());
            ps.setInt(7, s.isActive() ? 1 : 0);
            setNullableLong(ps, 8, s.getCreatedBy());
            ps.setString(9, DateTimeUtil.toDb(s.getCreatedAt()));
        });
        s.setId(id);
        return id;
    }

    @Override
    public void update(AttendanceSession s) {
        update("""
                UPDATE attendance_sessions
                SET title = ?, session_type = ?, venue = ?, start_time = ?, end_time = ?, active = ?
                WHERE id = ?
                """, ps -> {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getSessionType().name());
            ps.setString(3, s.getVenue());
            ps.setString(4, DateTimeUtil.toDb(s.getStartTime()));
            ps.setString(5, DateTimeUtil.toDb(s.getEndTime()));
            ps.setInt(6, s.isActive() ? 1 : 0);
            ps.setLong(7, s.getId());
        });
    }

    @Override
    public void updateNonce(long sessionId, String nonce) {
        update("UPDATE attendance_sessions SET qr_nonce = ? WHERE id = ?", ps -> {
            ps.setString(1, nonce);
            ps.setLong(2, sessionId);
        });
    }

    @Override
    public void setActive(long sessionId, boolean active) {
        update("UPDATE attendance_sessions SET active = ? WHERE id = ?", ps -> {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, sessionId);
        });
    }

    @Override
    public Optional<AttendanceSession> findById(long id) {
        return queryOne(SELECT + " WHERE s.id = ?", ps -> ps.setLong(1, id), JdbcAttendanceSessionDao::map);
    }

    @Override
    public List<AttendanceSession> findAll() {
        return query(SELECT + " ORDER BY s.start_time DESC", NO_PARAMS, JdbcAttendanceSessionDao::map);
    }

    @Override
    public boolean delete(long id) {
        return update("DELETE FROM attendance_sessions WHERE id = ?", ps -> ps.setLong(1, id)) > 0;
    }

    private static AttendanceSession map(ResultSet rs) throws SQLException {
        AttendanceSession s = new AttendanceSession();
        s.setId(rs.getLong("id"));
        s.setTitle(rs.getString("title"));
        s.setSessionType(SessionType.valueOf(rs.getString("session_type")));
        s.setVenue(rs.getString("venue"));
        s.setStartTime(getDateTime(rs, "start_time"));
        s.setEndTime(getDateTime(rs, "end_time"));
        s.setQrNonce(rs.getString("qr_nonce"));
        s.setActive(rs.getInt("active") == 1);
        s.setCreatedBy(getNullableLong(rs, "created_by"));
        s.setCreatedAt(getDateTime(rs, "created_at"));
        s.setAttendeeCount(rs.getInt("attendee_count"));
        return s;
    }
}
