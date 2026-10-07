package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.DisciplineDao;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.model.OffenseSeverity;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDisciplineDao extends AbstractJdbcDao implements DisciplineDao {
    private static final String SELECT = """
            SELECT d.id, d.student_id, d.incident_date, d.offense, d.severity, d.description, d.sanction,
                   d.status, d.resolution, d.recorded_by, d.recorded_at, d.resolved_at,
                   s.student_number, u.first_name || ' ' || u.last_name AS student_name,
                   a.first_name || ' ' || a.last_name AS recorded_by_name
            FROM discipline_records d
            JOIN users u ON u.id = d.student_id
            LEFT JOIN students s ON s.user_id = d.student_id
            LEFT JOIN users a ON a.id = d.recorded_by
            """;
    private static final String ORDER = " ORDER BY d.incident_date DESC, d.id DESC";
    private static final int MAX_LIST_ROWS = 2000;

    public JdbcDisciplineDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<DisciplineRecord> search(CaseStatus status, String search) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append(" AND d.status = ?");
            params.add(status);
        }
        if (search != null && !search.isBlank()) {
            String pattern = containsPattern(search);
            sql.append(" AND (LOWER(u.first_name) LIKE ? ESCAPE '\\' OR LOWER(u.last_name) LIKE ? ESCAPE '\\'"
                    + " OR LOWER(s.student_number) LIKE ? ESCAPE '\\' OR LOWER(d.offense) LIKE ? ESCAPE '\\')");
            for (int i = 0; i < 4; i++) {
                params.add(pattern);
            }
        }
        sql.append(ORDER).append(" LIMIT ").append(MAX_LIST_ROWS);
        return queryList(sql.toString(), JdbcDisciplineDao::map, params.toArray());
    }

    @Override
    public List<DisciplineRecord> findByStudent(long studentId) {
        return queryList(SELECT + " WHERE d.student_id = ?" + ORDER, JdbcDisciplineDao::map, studentId);
    }

    @Override
    public Optional<DisciplineRecord> findById(long id) {
        return queryOne(SELECT + " WHERE d.id = ?", JdbcDisciplineDao::map, id);
    }

    @Override
    public long countByStatus(CaseStatus status) {
        return queryOne("SELECT COUNT(*) FROM discipline_records WHERE status = ?", rs -> rs.getLong(1), status).orElse(0L);
    }

    @Override
    public long create(DisciplineRecord r) {
        long id = insert("""
                INSERT INTO discipline_records (student_id, incident_date, offense, severity, description, sanction,
                                                status, resolution, recorded_by, recorded_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                r.getStudentId(), r.getIncidentDate(), r.getOffense(), r.getSeverity(), r.getDescription(),
                r.getSanction(), r.getStatus(), r.getResolution(), r.getRecordedBy(), r.getRecordedAt(), r.getResolvedAt());
        r.setId(id);
        return id;
    }

    @Override
    public void update(DisciplineRecord r) {
        update("""
                UPDATE discipline_records SET student_id = ?, incident_date = ?, offense = ?, severity = ?,
                       description = ?, sanction = ?, status = ?, resolution = ?, resolved_at = ?
                WHERE id = ?
                """,
                r.getStudentId(), r.getIncidentDate(), r.getOffense(), r.getSeverity(), r.getDescription(),
                r.getSanction(), r.getStatus(), r.getResolution(), r.getResolvedAt(), r.getId());
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM discipline_records WHERE id = ?", id);
    }

    private static DisciplineRecord map(ResultSet rs) throws SQLException {
        DisciplineRecord r = new DisciplineRecord();
        r.setId(rs.getLong("id"));
        r.setStudentId(rs.getLong("student_id"));
        r.setStudentNumber(rs.getString("student_number"));
        r.setStudentName(rs.getString("student_name"));
        r.setIncidentDate(getDate(rs, "incident_date"));
        r.setOffense(rs.getString("offense"));
        r.setSeverity(OffenseSeverity.valueOf(rs.getString("severity")));
        r.setDescription(rs.getString("description"));
        r.setSanction(rs.getString("sanction"));
        r.setStatus(CaseStatus.valueOf(rs.getString("status")));
        r.setResolution(rs.getString("resolution"));
        r.setRecordedBy(getNullableLong(rs, "recorded_by"));
        r.setRecordedByName(rs.getString("recorded_by_name"));
        r.setRecordedAt(getDateTime(rs, "recorded_at"));
        r.setResolvedAt(getDateTime(rs, "resolved_at"));
        return r;
    }
}
