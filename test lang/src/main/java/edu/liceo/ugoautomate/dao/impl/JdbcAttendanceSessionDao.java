package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.SessionType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcAttendanceSessionDao extends AbstractJdbcDao implements AttendanceSessionDao {
    private static final String SELECT = """
            SELECT s.id, s.title, s.session_type, s.location_id, l.name AS location_name,
                   s.starts_at, s.ends_at, s.created_by, s.created_at,
                   (SELECT COUNT(*) FROM attendance_records r WHERE r.session_id = s.id) AS attendee_count
            FROM attendance_sessions s
            LEFT JOIN locations l ON l.id = s.location_id
            """;

    public JdbcAttendanceSessionDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<AttendanceSession> findAll() {
        return queryList(SELECT + " ORDER BY s.starts_at DESC LIMIT 1000", JdbcAttendanceSessionDao::map);
    }

    @Override
    public Optional<AttendanceSession> findById(long id) {
        return queryOne(SELECT + " WHERE s.id = ?", JdbcAttendanceSessionDao::map, id);
    }

    @Override
    public long create(AttendanceSession s) {
        long id = insert("""
                INSERT INTO attendance_sessions (title, session_type, location_id, starts_at, ends_at, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                s.getTitle(), s.getType(), s.getLocationId(), s.getStartsAt(), s.getEndsAt(),
                s.getCreatedBy(), s.getCreatedAt());
        s.setId(id);
        return id;
    }

    @Override
    public void update(AttendanceSession s) {
        update("""
                UPDATE attendance_sessions SET title = ?, session_type = ?, location_id = ?, starts_at = ?, ends_at = ?
                WHERE id = ?
                """,
                s.getTitle(), s.getType(), s.getLocationId(), s.getStartsAt(), s.getEndsAt(), s.getId());
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM attendance_sessions WHERE id = ?", id);
    }

    private static AttendanceSession map(ResultSet rs) throws SQLException {
        AttendanceSession s = new AttendanceSession();
        s.setId(rs.getLong("id"));
        s.setTitle(rs.getString("title"));
        s.setType(SessionType.valueOf(rs.getString("session_type")));
        s.setLocationId(getNullableLong(rs, "location_id"));
        s.setLocationName(rs.getString("location_name"));
        s.setStartsAt(getDateTime(rs, "starts_at"));
        s.setEndsAt(getDateTime(rs, "ends_at"));
        s.setCreatedBy(getNullableLong(rs, "created_by"));
        s.setCreatedAt(getDateTime(rs, "created_at"));
        s.setAttendeeCount(rs.getInt("attendee_count"));
        return s;
    }
}
