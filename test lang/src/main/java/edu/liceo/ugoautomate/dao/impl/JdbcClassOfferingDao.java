package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.ClassOfferingDao;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.EnrolledStudent;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcClassOfferingDao extends AbstractJdbcDao implements ClassOfferingDao {
    private static final String SELECT = """
            SELECT c.id, c.term, c.subject_code, c.subject_title, c.units, c.section, c.faculty_id,
                   c.days, c.start_time, c.end_time, c.room,
                   f.first_name || ' ' || f.last_name AS faculty_name,
                   (SELECT COUNT(*) FROM class_enrollments e WHERE e.class_id = c.id) AS enrolled_count
            FROM class_offerings c
            LEFT JOIN faculty f ON f.id = c.faculty_id
            """;
    private static final String ORDER = " ORDER BY LOWER(c.subject_code), LOWER(c.section)";
    private static final String STUDENT_SELECT = """
            SELECT u.id, s.student_number, u.first_name || ' ' || u.last_name AS name, s.course, s.year_level
            FROM students s JOIN users u ON u.id = s.user_id
            """;

    public JdbcClassOfferingDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<ClassOffering> findByTerm(String term) {
        return queryList(SELECT + " WHERE c.term = ?" + ORDER, JdbcClassOfferingDao::map, term);
    }

    @Override
    public Optional<ClassOffering> findById(long id) {
        return queryOne(SELECT + " WHERE c.id = ?", JdbcClassOfferingDao::map, id);
    }

    @Override
    public List<ClassOffering> findByStudent(long studentId, String term) {
        return queryList(SELECT + " JOIN class_enrollments en ON en.class_id = c.id"
                + " WHERE en.student_id = ? AND c.term = ?" + ORDER, JdbcClassOfferingDao::map, studentId, term);
    }

    @Override
    public List<String> findTerms() {
        return queryList("SELECT term, MAX(id) AS latest FROM class_offerings GROUP BY term ORDER BY latest DESC",
                rs -> rs.getString("term"));
    }

    @Override
    public long create(ClassOffering c) {
        long id = insert("""
                INSERT INTO class_offerings (term, subject_code, subject_title, units, section, faculty_id,
                                             days, start_time, end_time, room)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                c.getTerm(), c.getSubjectCode(), c.getSubjectTitle(), c.getUnits(), c.getSection(), c.getFacultyId(),
                daysText(c.getDays()), time(c.getStartTime()), time(c.getEndTime()), c.getRoom());
        c.setId(id);
        return id;
    }

    @Override
    public void update(ClassOffering c) {
        update("""
                UPDATE class_offerings SET term = ?, subject_code = ?, subject_title = ?, units = ?, section = ?,
                       faculty_id = ?, days = ?, start_time = ?, end_time = ?, room = ?
                WHERE id = ?
                """,
                c.getTerm(), c.getSubjectCode(), c.getSubjectTitle(), c.getUnits(), c.getSection(), c.getFacultyId(),
                daysText(c.getDays()), time(c.getStartTime()), time(c.getEndTime()), c.getRoom(), c.getId());
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM class_offerings WHERE id = ?", id);
    }

    @Override
    public List<EnrolledStudent> findEnrolled(long classId) {
        return queryList(STUDENT_SELECT + " JOIN class_enrollments e ON e.student_id = u.id WHERE e.class_id = ?"
                + " ORDER BY LOWER(u.last_name), LOWER(u.first_name)", JdbcClassOfferingDao::mapStudent, classId);
    }

    @Override
    public boolean enroll(long classId, long studentId) {
        if (queryExists("SELECT 1 FROM class_enrollments WHERE class_id = ? AND student_id = ?", classId, studentId)) {
            return false;
        }
        update("INSERT INTO class_enrollments (class_id, student_id) VALUES (?, ?)", classId, studentId);
        return true;
    }

    @Override
    public void unenroll(long classId, long studentId) {
        update("DELETE FROM class_enrollments WHERE class_id = ? AND student_id = ?", classId, studentId);
    }

    @Override
    public List<EnrolledStudent> findStudents(String course, int yearLevel) {
        return queryList(STUDENT_SELECT + " WHERE LOWER(s.course) = LOWER(?) AND s.year_level = ? AND u.active = 1",
                JdbcClassOfferingDao::mapStudent, course, yearLevel);
    }

    @Override
    public Optional<EnrolledStudent> findStudentByNumber(String studentNumber) {
        return queryOne(STUDENT_SELECT + " WHERE s.student_number = ?", JdbcClassOfferingDao::mapStudent, studentNumber);
    }

    @Override
    public Set<Long> findFacultyIdsOfStudent(long studentId, String term) {
        return new HashSet<>(queryList("""
                SELECT DISTINCT c.faculty_id FROM class_offerings c
                JOIN class_enrollments e ON e.class_id = c.id
                WHERE e.student_id = ? AND c.term = ? AND c.faculty_id IS NOT NULL
                """, rs -> rs.getLong(1), studentId, term));
    }

    private static String daysText(Set<DayOfWeek> days) {
        return days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    private static String time(LocalTime time) {
        return time == null ? null : String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private static ClassOffering map(ResultSet rs) throws SQLException {
        ClassOffering c = new ClassOffering();
        c.setId(rs.getLong("id"));
        c.setTerm(rs.getString("term"));
        c.setSubjectCode(rs.getString("subject_code"));
        c.setSubjectTitle(rs.getString("subject_title"));
        c.setUnits(rs.getInt("units"));
        c.setSection(rs.getString("section"));
        c.setFacultyId(getNullableLong(rs, "faculty_id"));
        c.setFacultyName(rs.getString("faculty_name"));
        String days = rs.getString("days");
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        if (days != null && !days.isBlank()) {
            Arrays.stream(days.split(",")).map(String::trim).map(DayOfWeek::valueOf).forEach(set::add);
        }
        c.setDays(set);
        c.setStartTime(LocalTime.parse(rs.getString("start_time")));
        c.setEndTime(LocalTime.parse(rs.getString("end_time")));
        c.setRoom(rs.getString("room"));
        c.setEnrolledCount(rs.getInt("enrolled_count"));
        return c;
    }

    private static EnrolledStudent mapStudent(ResultSet rs) throws SQLException {
        return new EnrolledStudent(rs.getLong("id"), rs.getString("student_number"), rs.getString("name"),
                rs.getString("course"), rs.getInt("year_level"));
    }
}
