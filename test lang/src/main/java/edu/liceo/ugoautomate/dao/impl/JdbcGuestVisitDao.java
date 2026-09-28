package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link GuestVisitDao}.
 */
public class JdbcGuestVisitDao extends AbstractJdbcDao implements GuestVisitDao {

    private static final String SELECT = """
            SELECT v.id, v.guest_user_id, v.purpose, v.person_to_visit, v.visit_date, v.status, v.pass_nonce,
                   v.registered_at, v.checked_in_at,
                   u.full_name AS guest_name, u.contact_number AS guest_contact
            FROM guest_visits v
            JOIN users u ON u.id = v.guest_user_id
            """;

    public JdbcGuestVisitDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long insert(GuestVisit v) {
        long id = insert("""
                INSERT INTO guest_visits
                    (guest_user_id, purpose, person_to_visit, visit_date, status, pass_nonce, registered_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setLong(1, v.getGuestUserId());
            ps.setString(2, v.getPurpose());
            ps.setString(3, v.getPersonToVisit());
            ps.setString(4, DateTimeUtil.toDb(v.getVisitDate()));
            ps.setString(5, v.getStatus().name());
            ps.setString(6, v.getPassNonce());
            ps.setString(7, DateTimeUtil.toDb(v.getRegisteredAt()));
        });
        v.setId(id);
        return id;
    }

    @Override
    public Optional<GuestVisit> findById(long id) {
        return queryOne(SELECT + " WHERE v.id = ?", ps -> ps.setLong(1, id), JdbcGuestVisitDao::map);
    }

    @Override
    public List<GuestVisit> findByGuest(long guestUserId) {
        return query(SELECT + " WHERE v.guest_user_id = ? ORDER BY v.visit_date DESC, v.registered_at DESC",
                ps -> ps.setLong(1, guestUserId), JdbcGuestVisitDao::map);
    }

    @Override
    public Optional<GuestVisit> findRegisteredForGuestOn(long guestUserId, LocalDate date) {
        return queryOne(SELECT + " WHERE v.guest_user_id = ? AND v.visit_date = ? AND v.status = 'REGISTERED'",
                ps -> {
                    ps.setLong(1, guestUserId);
                    ps.setString(2, DateTimeUtil.toDb(date));
                }, JdbcGuestVisitDao::map);
    }

    @Override
    public List<GuestVisit> findByDate(LocalDate date) {
        return query(SELECT + " WHERE v.visit_date = ? ORDER BY v.registered_at DESC",
                ps -> ps.setString(1, DateTimeUtil.toDb(date)), JdbcGuestVisitDao::map);
    }

    @Override
    public List<GuestVisit> findRecent(int limit) {
        return query(SELECT + " ORDER BY v.registered_at DESC LIMIT ?",
                ps -> ps.setInt(1, limit), JdbcGuestVisitDao::map);
    }

    @Override
    public boolean markCheckedIn(long visitId, LocalDateTime checkedInAt) {
        return update("""
                UPDATE guest_visits SET status = 'CHECKED_IN', checked_in_at = ?
                WHERE id = ? AND status = 'REGISTERED'
                """, ps -> {
            ps.setString(1, DateTimeUtil.toDb(checkedInAt));
            ps.setLong(2, visitId);
        }) == 1;
    }

    @Override
    public void updateStatus(long visitId, VisitStatus status) {
        update("UPDATE guest_visits SET status = ? WHERE id = ?", ps -> {
            ps.setString(1, status.name());
            ps.setLong(2, visitId);
        });
    }

    private static GuestVisit map(ResultSet rs) throws SQLException {
        GuestVisit v = new GuestVisit();
        v.setId(rs.getLong("id"));
        v.setGuestUserId(rs.getLong("guest_user_id"));
        v.setPurpose(rs.getString("purpose"));
        v.setPersonToVisit(rs.getString("person_to_visit"));
        v.setVisitDate(DateTimeUtil.dateFromDb(rs.getString("visit_date")));
        v.setStatus(VisitStatus.valueOf(rs.getString("status")));
        v.setPassNonce(rs.getString("pass_nonce"));
        v.setRegisteredAt(getDateTime(rs, "registered_at"));
        v.setCheckedInAt(getDateTime(rs, "checked_in_at"));
        v.setGuestName(rs.getString("guest_name"));
        v.setGuestContact(rs.getString("guest_contact"));
        return v;
    }
}
