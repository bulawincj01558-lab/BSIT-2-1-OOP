package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.model.EntrantType;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.VerificationMethod;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JdbcEntryLogDao extends AbstractJdbcDao implements EntryLogDao {
    public JdbcEntryLogDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long create(EntryLog e) {
        long id = insert("""
                INSERT INTO entry_logs (entrant_type, user_id, visit_id, entrant_name, details, method, verified_by, logged_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                e.getEntrantType(), e.getUserId(), e.getVisitId(), e.getEntrantName(), e.getDetails(),
                e.getMethod(), e.getVerifiedBy(), e.getLoggedAt());
        e.setId(id);
        return id;
    }

    @Override
    public List<EntryLog> findBetween(LocalDate from, LocalDate to) {
        return queryList("""
                SELECT e.id, e.entrant_type, e.user_id, e.visit_id, e.entrant_name, e.details, e.method,
                       e.verified_by, e.logged_at, a.first_name || ' ' || a.last_name AS verified_by_name
                FROM entry_logs e
                LEFT JOIN users a ON a.id = e.verified_by
                WHERE e.logged_at >= ? AND e.logged_at < ?
                ORDER BY e.logged_at DESC
                """,
                JdbcEntryLogDao::map, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private static EntryLog map(ResultSet rs) throws SQLException {
        EntryLog e = new EntryLog();
        e.setId(rs.getLong("id"));
        e.setEntrantType(EntrantType.valueOf(rs.getString("entrant_type")));
        e.setUserId(getNullableLong(rs, "user_id"));
        e.setVisitId(getNullableLong(rs, "visit_id"));
        e.setEntrantName(rs.getString("entrant_name"));
        e.setDetails(rs.getString("details"));
        e.setMethod(VerificationMethod.valueOf(rs.getString("method")));
        e.setVerifiedBy(getNullableLong(rs, "verified_by"));
        e.setVerifiedByName(rs.getString("verified_by_name"));
        e.setLoggedAt(getDateTime(rs, "logged_at"));
        return e;
    }
}
