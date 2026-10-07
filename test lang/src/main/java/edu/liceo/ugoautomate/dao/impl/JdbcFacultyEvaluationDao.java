package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.FacultyEvaluationDao;
import edu.liceo.ugoautomate.model.EvaluationCriterion;
import edu.liceo.ugoautomate.model.EvaluationSummary;
import edu.liceo.ugoautomate.model.FacultyEvaluation;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JdbcFacultyEvaluationDao extends AbstractJdbcDao implements FacultyEvaluationDao {
    private static final List<EvaluationCriterion> CRITERIA = Arrays.asList(EvaluationCriterion.values());
    private static final String RATING_COLUMNS = CRITERIA.stream()
            .map(c -> "e." + c.getColumn()).collect(Collectors.joining(", "));
    private static final String SELECT = "SELECT e.id, e.faculty_id, e.student_id, e.term, " + RATING_COLUMNS
            + ", e.comment, e.submitted_at, f.first_name || ' ' || f.last_name AS faculty_name"
            + " FROM faculty_evaluations e JOIN faculty f ON f.id = e.faculty_id";

    public JdbcFacultyEvaluationDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public long create(FacultyEvaluation e) {
        String columns = CRITERIA.stream().map(EvaluationCriterion::getColumn).collect(Collectors.joining(", "));
        String marks = CRITERIA.stream().map(c -> "?").collect(Collectors.joining(", "));
        List<Object> params = new ArrayList<>(List.of(e.getFacultyId(), e.getStudentId(), e.getTerm()));
        CRITERIA.forEach(c -> params.add(e.getRating(c)));
        params.add(e.getComment());
        params.add(e.getSubmittedAt());
        long id = insert("INSERT INTO faculty_evaluations (faculty_id, student_id, term, " + columns
                        + ", comment, submitted_at) VALUES (?, ?, ?, " + marks + ", ?, ?)",
                params.toArray());
        e.setId(id);
        return id;
    }

    @Override
    public boolean exists(long facultyId, long studentId, String term) {
        return queryExists("SELECT 1 FROM faculty_evaluations WHERE faculty_id = ? AND student_id = ? AND term = ?",
                facultyId, studentId, term);
    }

    @Override
    public List<FacultyEvaluation> findByStudent(long studentId, String term) {
        return queryList(SELECT + " WHERE e.student_id = ? AND e.term = ? ORDER BY e.submitted_at DESC",
                JdbcFacultyEvaluationDao::map, studentId, term);
    }

    @Override
    public List<EvaluationSummary> summarize(String term) {
        String averages = CRITERIA.stream()
                .map(c -> "AVG(e." + c.getColumn() + ") AS avg_" + c.getColumn())
                .collect(Collectors.joining(", "));

        return queryList("SELECT f.id, f.first_name, f.last_name, f.department, f.email, f.active, "
                        + "COUNT(e.id) AS evaluation_count, " + averages
                        + " FROM faculty f LEFT JOIN faculty_evaluations e ON e.faculty_id = f.id AND e.term = ?"
                        + " GROUP BY f.id, f.first_name, f.last_name, f.department, f.email, f.active"
                        + " ORDER BY LOWER(f.last_name), LOWER(f.first_name)",
                rs -> {
                    int count = rs.getInt("evaluation_count");
                    Map<EvaluationCriterion, Double> avg = new EnumMap<>(EvaluationCriterion.class);
                    if (count > 0) {
                        for (EvaluationCriterion c : CRITERIA) {
                            avg.put(c, rs.getDouble("avg_" + c.getColumn()));
                        }
                    }
                    return new EvaluationSummary(JdbcFacultyDao.map(rs), count, avg);
                }, term);
    }

    @Override
    public List<FacultyEvaluation> findByFaculty(long facultyId, String term) {
        return queryList(SELECT + " WHERE e.faculty_id = ? AND e.term = ? ORDER BY e.submitted_at DESC",
                JdbcFacultyEvaluationDao::map, facultyId, term);
    }

    @Override
    public List<String> findTerms() {
        return queryList("SELECT term, MAX(submitted_at) AS latest FROM faculty_evaluations GROUP BY term ORDER BY latest DESC",
                rs -> rs.getString("term"));
    }

    private static FacultyEvaluation map(ResultSet rs) throws SQLException {
        FacultyEvaluation e = new FacultyEvaluation();
        e.setId(rs.getLong("id"));
        e.setFacultyId(rs.getLong("faculty_id"));
        e.setStudentId(rs.getLong("student_id"));
        e.setTerm(rs.getString("term"));
        for (EvaluationCriterion c : CRITERIA) {
            e.setRating(c, rs.getInt(c.getColumn()));
        }
        e.setComment(rs.getString("comment"));
        e.setSubmittedAt(getDateTime(rs, "submitted_at"));
        e.setFacultyName(rs.getString("faculty_name"));
        return e;
    }
}
