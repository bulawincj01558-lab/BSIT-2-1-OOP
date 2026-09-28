package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.model.EntrantType;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SQLite implementation of {@link EntryLogDao}. Date-range queries use the
 * entry_time index.
 */
public class JdbcEntryLogDao extends AbstractJdbcDao implements EntryLogDao {

    public JdbcEntryLogDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long insert(EntryLog log) {
        long id = insert("""
                INSERT INTO entry_logs
                    (entrant_type, user_id, visit_id, entrant_name, identifier, details,
                     verification_method, verified_by, entry_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setString(1, log.getEntrantType().name());
            setNullableLong(ps, 2, log.getUserId());
            setNullableLong(ps, 3, log.getVisitId());
            ps.setString(4, log.getEntrantName());
            ps.setString(5, log.getIdentifier());
            ps.setString(6, log.getDetails());
            ps.setString(7, log.getMethod().name());
            setNullableLong(ps, 8, log.getVerifiedBy());
            ps.setString(9, DateTimeUtil.toDb(log.getEntryTime()));
        });
        log.setId(id);
        return id;
    }

    @Override
    public List<EntryLog> findBetween(LocalDateTime from, LocalDateTime to) {
        return query("""
                SELECT e.*, vb.full_name AS verified_by_name
                FROM entry_logs e
                LEFT JOIN users vb ON vb.id = e.verified_by
                WHERE e.entry_time BETWEEN ? AND ?
                ORDER BY e.entry_time DESC
                """, ps -> {
            ps.setString(1, DateTimeUtil.toDb(from));
            ps.setString(2, DateTimeUtil.toDb(to));
        }, JdbcEntryLogDao::map);
    }

    private static EntryLog map(ResultSet rs) throws SQLException {
        EntryLog log = new EntryLog();
        log.setId(rs.getLong("id"));
        log.setEntrantType(EntrantType.valueOf(rs.getString("entrant_type")));
        log.setUserId(getNullableLong(rs, "user_id"));
        log.setVisitId(getNullableLong(rs, "visit_id"));
        log.setEntrantName(rs.getString("entrant_name"));
        log.setIdentifier(rs.getString("identifier"));
        log.setDetails(rs.getString("details"));
        log.setMethod(VerificationMethod.valueOf(rs.getString("verification_method")));
        log.setVerifiedBy(getNullableLong(rs, "verified_by"));
        log.setVerifiedByName(rs.getString("verified_by_name"));
        log.setEntryTime(getDateTime(rs, "entry_time"));
        return log;
    }
}
