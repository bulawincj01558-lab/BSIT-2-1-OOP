package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.TuitionDao;
import edu.liceo.ugoautomate.model.TuitionAccount;
import edu.liceo.ugoautomate.model.TuitionEntry;
import edu.liceo.ugoautomate.model.TuitionEntryType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcTuitionDao extends AbstractJdbcDao implements TuitionDao {
    private static final String SELECT = """
            SELECT t.id, t.student_id, t.term, t.entry_type, t.description, t.amount_cents, t.reference,
                   t.entry_date, t.recorded_by, t.recorded_at, a.first_name || ' ' || a.last_name AS recorded_by_name
            FROM tuition_entries t
            LEFT JOIN users a ON a.id = t.recorded_by
            """;

    public JdbcTuitionDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<TuitionAccount> findAccounts(String term, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, s.student_number, u.first_name || ' ' || u.last_name AS name, s.course, s.year_level,
                       COALESCE(SUM(CASE WHEN t.entry_type = 'CHARGE' THEN t.amount_cents END), 0) AS charges,
                       COALESCE(SUM(CASE WHEN t.entry_type = 'PAYMENT' THEN t.amount_cents END), 0) AS payments,
                       COALESCE(SUM(CASE WHEN t.entry_type = 'DISCOUNT' THEN t.amount_cents END), 0) AS discounts
                FROM students s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN tuition_entries t ON t.student_id = u.id AND t.term = ?
                WHERE u.active = 1
                """);
        List<Object> params = new ArrayList<>(List.of(term));
        if (search != null && !search.isBlank()) {
            String pattern = containsPattern(search);
            sql.append(" AND (LOWER(u.first_name) LIKE ? ESCAPE '\\' OR LOWER(u.last_name) LIKE ? ESCAPE '\\'"
                    + " OR LOWER(s.student_number) LIKE ? ESCAPE '\\')");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" GROUP BY u.id, s.student_number, u.first_name, u.last_name, s.course, s.year_level")
                .append(" ORDER BY LOWER(u.last_name), LOWER(u.first_name)");
        return queryList(sql.toString(), rs -> new TuitionAccount(rs.getLong("id"), rs.getString("student_number"),
                rs.getString("name"), rs.getString("course"), rs.getInt("year_level"),
                rs.getLong("charges"), rs.getLong("payments"), rs.getLong("discounts")), params.toArray());
    }

    @Override
    public List<TuitionEntry> findEntries(long studentId, String term) {
        return queryList(SELECT + " WHERE t.student_id = ? AND t.term = ? ORDER BY t.entry_date, t.id",
                JdbcTuitionDao::map, studentId, term);
    }

    @Override
    public Optional<TuitionEntry> findById(long id) {
        return queryOne(SELECT + " WHERE t.id = ?", JdbcTuitionDao::map, id);
    }

    @Override
    public List<String> findTerms() {
        return queryList("SELECT term, MAX(id) AS latest FROM tuition_entries GROUP BY term ORDER BY latest DESC",
                rs -> rs.getString("term"));
    }

    @Override
    public Map<Long, Integer> findEnrolledUnits(String term) {
        Map<Long, Integer> units = new LinkedHashMap<>();
        queryList("""
                SELECT e.student_id, SUM(c.units) AS units
                FROM class_enrollments e
                JOIN class_offerings c ON c.id = e.class_id
                JOIN users u ON u.id = e.student_id
                WHERE c.term = ? AND u.active = 1
                GROUP BY e.student_id
                """, rs -> units.put(rs.getLong("student_id"), rs.getInt("units")), term);
        return units;
    }

    @Override
    public boolean hasCharge(long studentId, String term, String prefix) {
        return queryExists("SELECT 1 FROM tuition_entries WHERE student_id = ? AND term = ? AND entry_type = 'CHARGE'"
                + " AND description LIKE ? ESCAPE '\\'", studentId, term, prefix.replace("%", "\\%").replace("_", "\\_") + "%");
    }

    @Override
    public long create(TuitionEntry e) {
        long id = insert("""
                INSERT INTO tuition_entries (student_id, term, entry_type, description, amount_cents, reference,
                                             entry_date, recorded_by, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                e.getStudentId(), e.getTerm(), e.getType(), e.getDescription(), e.getAmountCents(), e.getReference(),
                e.getEntryDate(), e.getRecordedBy(), e.getRecordedAt());
        e.setId(id);
        return id;
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM tuition_entries WHERE id = ?", id);
    }

    private static TuitionEntry map(ResultSet rs) throws SQLException {
        TuitionEntry e = new TuitionEntry();
        e.setId(rs.getLong("id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setTerm(rs.getString("term"));
        e.setType(TuitionEntryType.valueOf(rs.getString("entry_type")));
        e.setDescription(rs.getString("description"));
        e.setAmountCents(rs.getLong("amount_cents"));
        e.setReference(rs.getString("reference"));
        e.setEntryDate(getDate(rs, "entry_date"));
        e.setRecordedBy(getNullableLong(rs, "recorded_by"));
        e.setRecordedByName(rs.getString("recorded_by_name"));
        e.setRecordedAt(getDateTime(rs, "recorded_at"));
        return e;
    }
}
