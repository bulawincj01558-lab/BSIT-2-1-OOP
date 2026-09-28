package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link UserDao}. Uses a single LEFT JOIN query over
 * the base table and the three subtype tables, then instantiates the concrete
 * subclass that matches the stored role.
 */
public class JdbcUserDao extends AbstractJdbcDao implements UserDao {

    private static final String SELECT = """
            SELECT u.id, u.username, u.password_hash, u.role, u.full_name, u.email, u.contact_number,
                   u.active, u.created_at,
                   s.student_number, s.course, s.year_level,
                   g.purpose_of_visit, g.person_to_visit,
                   a.office
            FROM users u
            LEFT JOIN students s ON s.user_id = u.id
            LEFT JOIN guests g ON g.user_id = u.id
            LEFT JOIN administrators a ON a.user_id = u.id
            """;

    private static final String INSERT_USER = """
            INSERT INTO users (username, password_hash, role, full_name, email, contact_number, active, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public JdbcUserDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<User> findById(long id) {
        return queryOne(SELECT + " WHERE u.id = ?", ps -> ps.setLong(1, id), JdbcUserDao::mapUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return queryOne(SELECT + " WHERE u.username = ?", ps -> ps.setString(1, username), JdbcUserDao::mapUser);
    }

    @Override
    public Optional<Student> findStudentByNumber(String studentNumber) {
        return queryOne(SELECT + " WHERE s.student_number = ?", ps -> ps.setString(1, studentNumber),
                JdbcUserDao::mapUser)
                .filter(Student.class::isInstance)
                .map(Student.class::cast);
    }

    @Override
    public List<Student> findAllStudents() {
        return query(SELECT + " WHERE u.role = 'STUDENT' ORDER BY u.full_name", NO_PARAMS, JdbcUserDao::mapUser)
                .stream().map(Student.class::cast).toList();
    }

    @Override
    public List<Guest> findAllGuests() {
        return query(SELECT + " WHERE u.role = 'GUEST' ORDER BY u.full_name", NO_PARAMS, JdbcUserDao::mapUser)
                .stream().map(Guest.class::cast).toList();
    }

    @Override
    public boolean usernameExists(String username) {
        return queryLong("SELECT COUNT(*) FROM users WHERE username = ?", ps -> ps.setString(1, username)) > 0;
    }

    @Override
    public boolean studentNumberExists(String studentNumber) {
        return queryLong("SELECT COUNT(*) FROM students WHERE student_number = ?",
                ps -> ps.setString(1, studentNumber)) > 0;
    }

    @Override
    public long countByRole(Role role) {
        return queryLong("SELECT COUNT(*) FROM users WHERE role = ?", ps -> ps.setString(1, role.name()));
    }

    @Override
    public long insertStudent(Student student) {
        return inTransaction(c -> {
            long id = insertBase(c, student);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO students (user_id, student_number, course, year_level) VALUES (?, ?, ?, ?)")) {
                ps.setLong(1, id);
                ps.setString(2, student.getStudentNumber());
                ps.setString(3, student.getCourse());
                ps.setInt(4, student.getYearLevel());
                ps.executeUpdate();
            }
            return id;
        });
    }

    @Override
    public long insertGuest(Guest guest) {
        return inTransaction(c -> {
            long id = insertBase(c, guest);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO guests (user_id, purpose_of_visit, person_to_visit) VALUES (?, ?, ?)")) {
                ps.setLong(1, id);
                ps.setString(2, guest.getPurposeOfVisit());
                ps.setString(3, guest.getPersonToVisit());
                ps.executeUpdate();
            }
            return id;
        });
    }

    @Override
    public long insertAdministrator(Administrator admin) {
        return inTransaction(c -> {
            long id = insertBase(c, admin);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO administrators (user_id, office) VALUES (?, ?)")) {
                ps.setLong(1, id);
                ps.setString(2, admin.getOffice());
                ps.executeUpdate();
            }
            return id;
        });
    }

    @Override
    public void updateProfile(User user) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET full_name = ?, email = ?, contact_number = ? WHERE id = ?")) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getContactNumber());
                ps.setLong(4, user.getId());
                ps.executeUpdate();
            }
            if (user instanceof Student s) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE students SET course = ?, year_level = ? WHERE user_id = ?")) {
                    ps.setString(1, s.getCourse());
                    ps.setInt(2, s.getYearLevel());
                    ps.setLong(3, s.getId());
                    ps.executeUpdate();
                }
            } else if (user instanceof Guest g) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE guests SET purpose_of_visit = ?, person_to_visit = ? WHERE user_id = ?")) {
                    ps.setString(1, g.getPurposeOfVisit());
                    ps.setString(2, g.getPersonToVisit());
                    ps.setLong(3, g.getId());
                    ps.executeUpdate();
                }
            } else if (user instanceof Administrator a) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE administrators SET office = ? WHERE user_id = ?")) {
                    ps.setString(1, a.getOffice());
                    ps.setLong(2, a.getId());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public void updatePasswordHash(long userId, String passwordHash) {
        update("UPDATE users SET password_hash = ? WHERE id = ?", ps -> {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
        });
    }

    @Override
    public void setActive(long userId, boolean active) {
        update("UPDATE users SET active = ? WHERE id = ?", ps -> {
            ps.setInt(1, active ? 1 : 0);
            ps.setLong(2, userId);
        });
    }

    @Override
    public boolean delete(long userId) {
        return update("DELETE FROM users WHERE id = ?", ps -> ps.setLong(1, userId)) > 0;
    }

    private long insertBase(Connection c, User user) throws SQLException {
        long id = insert(c, INSERT_USER, ps -> {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getContactNumber());
            ps.setInt(7, user.isActive() ? 1 : 0);
            ps.setString(8, DateTimeUtil.toDb(user.getCreatedAt()));
        });
        user.setId(id);
        return id;
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        User user = switch (role) {
            case STUDENT -> {
                Student s = new Student();
                s.setStudentNumber(rs.getString("student_number"));
                s.setCourse(rs.getString("course"));
                s.setYearLevel(rs.getInt("year_level"));
                yield s;
            }
            case GUEST -> {
                Guest g = new Guest();
                g.setPurposeOfVisit(rs.getString("purpose_of_visit"));
                g.setPersonToVisit(rs.getString("person_to_visit"));
                yield g;
            }
            case ADMIN -> {
                Administrator a = new Administrator();
                a.setOffice(rs.getString("office"));
                yield a;
            }
        };
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setContactNumber(rs.getString("contact_number"));
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(getDateTime(rs, "created_at"));
        return user;
    }
}
