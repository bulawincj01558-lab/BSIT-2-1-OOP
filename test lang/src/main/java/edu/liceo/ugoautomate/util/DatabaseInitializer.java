package edu.liceo.ugoautomate.util;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.PasswordHasher;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the schema on startup and seeds sample data into an empty database.
 * <p>
 * Seed accounts are created in Java (not SQL) so their passwords are hashed
 * with BCrypt at install time.
 */
public final class DatabaseInitializer {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    public static final String DEFAULT_STUDENT_PASSWORD = "Student@123";
    public static final String DEFAULT_GUEST_PASSWORD = "Guest@123";

    private final DataSource dataSource;
    private final UserDao userDao;
    private final Clock clock;

    public DatabaseInitializer(DataSource dataSource, UserDao userDao, Clock clock) {
        this.dataSource = dataSource;
        this.userDao = userDao;
        this.clock = clock;
    }

    public void initialize() {
        runScript("/db/schema.sql");
        dropLegacyMapColumns();
        if (count("campus_locations") == 0) {
            runScript("/db/seed.sql");
        }
        if (count("users") == 0) {
            seedAccounts();
        }
    }

    private void seedAccounts() {
        LocalDateTime now = LocalDateTime.now(clock);

        Administrator admin = new Administrator();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setFullName("System Administrator");
        admin.setEmail("admin@liceo.edu.ph");
        admin.setOffice("Campus Security Office");
        prepare(admin, DEFAULT_ADMIN_PASSWORD, now);
        userDao.insertAdministrator(admin);

        userDao.insertStudent(student("2023-00001", "Juan Dela Cruz", "BSIT", 2, "juan.delacruz@liceo.edu.ph", now));
        userDao.insertStudent(student("2023-00002", "Maria Clara Santos", "BSIT", 2, "maria.santos@liceo.edu.ph", now));
        userDao.insertStudent(student("2022-00015", "Jose Miguel Reyes", "BSCS", 3, "jose.reyes@liceo.edu.ph", now));
        userDao.insertStudent(student("2024-00107", "Angela Mae Villanueva", "BSN", 1, "angela.villanueva@liceo.edu.ph", now));

        Guest guest = new Guest();
        guest.setUsername("guest@example.com");
        guest.setFullName("Ana Lopez");
        guest.setEmail("guest@example.com");
        guest.setContactNumber("0917 123 4567");
        guest.setPurposeOfVisit("Inquire about admission requirements");
        guest.setPersonToVisit("Admissions Office");
        prepare(guest, DEFAULT_GUEST_PASSWORD, now);
        userDao.insertGuest(guest);
    }

    private static Student student(String number, String name, String course, int year, String email,
                                   LocalDateTime now) {
        Student s = new Student();
        s.setUsername(number);
        s.setStudentNumber(number);
        s.setFullName(name);
        s.setCourse(course);
        s.setYearLevel(year);
        s.setEmail(email);
        prepare(s, DEFAULT_STUDENT_PASSWORD, now);
        return s;
    }

    private static void prepare(User user, String password, LocalDateTime now) {
        user.setPasswordHash(PasswordHasher.hash(password.toCharArray()));
        user.setActive(true);
        user.setCreatedAt(now);
    }

    /**
     * Executes a classpath SQL script. Full-line {@code --} comments are
     * removed and statements are split on semicolons (the scripts contain no
     * semicolons inside string literals or triggers).
     */
    private void runScript(String resource) {
        List<String> statements = new ArrayList<>();
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new AppException("Missing database script " + resource);
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            for (String statement : sql.toString().split(";")) {
                if (!statement.isBlank()) {
                    statements.add(statement.trim());
                }
            }
        } catch (IOException e) {
            throw new AppException("Cannot read database script " + resource, e);
        }

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                for (String statement : statements) {
                    st.execute(statement);
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AppException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Databases created by earlier versions stored campus-map coordinates.
     * The map was removed, so those (NOT NULL) columns are dropped to keep
     * inserts working.
     */
    private void dropLegacyMapColumns() {
        List<String> legacy = List.of("map_x", "map_y", "map_width", "map_height");
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            List<String> present = new ArrayList<>();
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(campus_locations)")) {
                while (rs.next()) {
                    String column = rs.getString("name");
                    if (legacy.contains(column)) {
                        present.add(column);
                    }
                }
            }
            for (String column : present) {
                st.execute("ALTER TABLE campus_locations DROP COLUMN " + column);
            }
        } catch (SQLException e) {
            throw new AppException("Database upgrade failed: " + e.getMessage(), e);
        }
    }

    private long count(String table) {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new AppException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}
