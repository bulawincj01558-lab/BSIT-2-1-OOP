package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDao extends AbstractJdbcDao implements UserDao {
    private static final String SELECT = """
            SELECT u.id, u.role, u.email, u.password_hash, u.first_name, u.last_name, u.contact_number,
                   u.active, u.created_at,
                   s.student_number, s.course, s.year_level,
                   g.purpose_of_visit, g.person_to_visit,
                   a.position
            FROM users u
            LEFT JOIN students s ON s.user_id = u.id
            LEFT JOIN guests g ON g.user_id = u.id
            LEFT JOIN administrators a ON a.user_id = u.id
            """;

    private static final int MAX_LIST_ROWS = 1000;

    public JdbcUserDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<User> findById(long id) {
        return queryOne(SELECT + " WHERE u.id = ?", JdbcUserDao::mapUser, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryOne(SELECT + " WHERE u.email = ?", JdbcUserDao::mapUser, email);
    }

    @Override
    public Optional<User> findByStudentNumber(String studentNumber) {
        return queryOne(SELECT + " WHERE s.student_number = ?", JdbcUserDao::mapUser, studentNumber);
    }

    @Override
    public List<User> findAll(Role role, String search) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (role != null) {
            sql.append(" AND u.role = ?");
            params.add(role);
        }
        if (search != null && !search.isBlank()) {
            String pattern = containsPattern(search);
            sql.append(" AND (LOWER(u.first_name) LIKE ? ESCAPE '\\' OR LOWER(u.last_name) LIKE ? ESCAPE '\\'"
                    + " OR LOWER(u.email) LIKE ? ESCAPE '\\' OR LOWER(s.student_number) LIKE ? ESCAPE '\\')");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY LOWER(u.last_name), LOWER(u.first_name) LIMIT ").append(MAX_LIST_ROWS);
        return queryList(sql.toString(), JdbcUserDao::mapUser, params.toArray());
    }

    @Override
    public boolean emailExists(String email) {
        return queryExists("SELECT 1 FROM users WHERE email = ?", email);
    }

    @Override
    public boolean studentNumberExists(String studentNumber) {
        return queryExists("SELECT 1 FROM students WHERE student_number = ?", studentNumber);
    }

    @Override
    public long create(User user) {
        return inTransaction(connection -> {
            long id = insert(connection, """
                    INSERT INTO users (role, email, password_hash, first_name, last_name, contact_number, active, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    user.getRole(), user.getEmail(), user.getPasswordHash(), user.getFirstName(),
                    user.getLastName(), user.getContactNumber(), user.isActive(), user.getCreatedAt());
            insertRoleRow(connection, id, user);
            user.setId(id);
            return id;
        });
    }

    private static void insertRoleRow(Connection connection, long id, User user) throws SQLException {
        if (user instanceof Student student) {
            update(connection, "INSERT INTO students (user_id, student_number, course, year_level) VALUES (?, ?, ?, ?)",
                    id, student.getStudentNumber(), student.getCourse(), student.getYearLevel());
        } else if (user instanceof Guest guest) {
            update(connection, "INSERT INTO guests (user_id, purpose_of_visit, person_to_visit) VALUES (?, ?, ?)",
                    id, guest.getPurposeOfVisit(), guest.getPersonToVisit());
        } else if (user instanceof Administrator admin) {
            update(connection, "INSERT INTO administrators (user_id, position) VALUES (?, ?)",
                    id, admin.getPosition());
        }
    }

    @Override
    public void update(User user) {
        inTransaction(connection -> {
            update(connection, "UPDATE users SET first_name = ?, last_name = ?, contact_number = ?, active = ? WHERE id = ?",
                    user.getFirstName(), user.getLastName(), user.getContactNumber(), user.isActive(), user.getId());
            if (user instanceof Student student) {
                update(connection, "UPDATE students SET course = ?, year_level = ? WHERE user_id = ?",
                        student.getCourse(), student.getYearLevel(), student.getId());
            } else if (user instanceof Guest guest) {
                update(connection, "UPDATE guests SET purpose_of_visit = ?, person_to_visit = ? WHERE user_id = ?",
                        guest.getPurposeOfVisit(), guest.getPersonToVisit(), guest.getId());
            } else if (user instanceof Administrator admin) {
                update(connection, "UPDATE administrators SET position = ? WHERE user_id = ?",
                        admin.getPosition(), admin.getId());
            }
            return null;
        });
    }

    @Override
    public void updatePassword(long userId, String passwordHash) {
        update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    @Override
    public void setActive(long userId, boolean active) {
        update("UPDATE users SET active = ? WHERE id = ?", active, userId);
    }

    @Override
    public void delete(long userId) {
        update("DELETE FROM users WHERE id = ?", userId);
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
                a.setPosition(rs.getString("position"));
                yield a;
            }
        };
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setContactNumber(rs.getString("contact_number"));
        user.setActive(rs.getInt("active") == 1);
        user.setCreatedAt(getDateTime(rs, "created_at"));
        return user;
    }
}
