package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.VisitStatus;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JdbcGuestVisitDao extends AbstractJdbcDao implements GuestVisitDao {
    private static final String SELECT = """
            SELECT v.id, v.guest_id, v.purpose, v.person_to_visit, v.visit_date, v.status,
                   v.registered_at, v.checked_in_at, v.checked_out_at,
                   u.first_name || ' ' || u.last_name AS guest_name, u.contact_number AS guest_contact
            FROM guest_visits v
            JOIN users u ON u.id = v.guest_id
            """;

    public JdbcGuestVisitDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long create(GuestVisit v) {
        long id = insert("""
                INSERT INTO guest_visits (guest_id, purpose, person_to_visit, visit_date, status, registered_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                v.getGuestId(), v.getPurpose(), v.getPersonToVisit(), v.getVisitDate(), v.getStatus(), v.getRegisteredAt());
        v.setId(id);
        return id;
    }

    @Override
    public Optional<GuestVisit> findById(long id) {
        return queryOne(SELECT + " WHERE v.id = ?", JdbcGuestVisitDao::map, id);
    }

    @Override
    public List<GuestVisit> findByGuest(long guestId) {
        return queryList(SELECT + " WHERE v.guest_id = ? ORDER BY v.visit_date DESC, v.id DESC",
                JdbcGuestVisitDao::map, guestId);
    }

    @Override
    public List<GuestVisit> findAll(LocalDate date) {
        if (date == null) {
            return queryList(SELECT + " ORDER BY v.visit_date DESC, v.id DESC LIMIT 2000", JdbcGuestVisitDao::map);
        }
        return queryList(SELECT + " WHERE v.visit_date = ? ORDER BY v.id DESC", JdbcGuestVisitDao::map, date);
    }

    @Override
    public void updateStatus(long visitId, VisitStatus status, LocalDateTime at) {
        switch (status) {
            case CHECKED_IN -> update("UPDATE guest_visits SET status = ?, checked_in_at = ? WHERE id = ?", status, at, visitId);
            case CHECKED_OUT -> update("UPDATE guest_visits SET status = ?, checked_out_at = ? WHERE id = ?", status, at, visitId);
            default -> update("UPDATE guest_visits SET status = ? WHERE id = ?", status, visitId);
        }
    }

    private static GuestVisit map(ResultSet rs) throws SQLException {
        GuestVisit v = new GuestVisit();
        v.setId(rs.getLong("id"));
        v.setGuestId(rs.getLong("guest_id"));
        v.setPurpose(rs.getString("purpose"));
        v.setPersonToVisit(rs.getString("person_to_visit"));
        v.setVisitDate(getDate(rs, "visit_date"));
        v.setStatus(VisitStatus.valueOf(rs.getString("status")));
        v.setRegisteredAt(getDateTime(rs, "registered_at"));
        v.setCheckedInAt(getDateTime(rs, "checked_in_at"));
        v.setCheckedOutAt(getDateTime(rs, "checked_out_at"));
        v.setGuestName(rs.getString("guest_name"));
        v.setGuestContact(rs.getString("guest_contact"));
        return v;
    }
}
