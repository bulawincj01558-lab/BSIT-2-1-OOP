package edu.liceo.ugoautomate.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

public final class Database implements AutoCloseable {
    public enum Dialect {
        SQLITE("sqlite"),
        POSTGRES("postgres");

        private final String folder;

        Dialect(String folder) {
            this.folder = folder;
        }
    }

    private static final String SCHEMA_HASH_KEY = "schema.hash";
    private static final String LOCK_TIMEOUT = "10s";

    private static final String LOCK_NOT_AVAILABLE = "55P03";
    private static final String QUERY_CANCELED = "57014";

    private final HikariDataSource dataSource;
    private final Dialect dialect;

    private Database(HikariDataSource dataSource, Dialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    public static Database open(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create database folder for " + file, e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file.toAbsolutePath());
        config.setPoolName("ugo-pool");
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(3_000);
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        config.setConnectionInitSql("PRAGMA foreign_keys = ON");
        return start(config, Dialect.SQLITE);
    }

    public static Database openPostgres(String jdbcUrl, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setPoolName("ugo-pool");

        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(15_000);

        config.addDataSourceProperty("prepareThreshold", "0");

        config.setConnectionInitSql("SET TIME ZONE '" + ZoneId.systemDefault().getId() + "'");
        return start(config, Dialect.POSTGRES);
    }

    private static Database start(HikariConfig config, Dialect dialect) {
        Database database = new Database(new HikariDataSource(config), dialect);
        try {
            database.initialize();
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
        return database;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Dialect getDialect() {
        return dialect;
    }

    private void initialize() {
        try (Connection connection = dataSource.getConnection()) {
            if (dialect == Dialect.POSTGRES) {
                execute(connection, "SET lock_timeout = '" + LOCK_TIMEOUT + "'");
            }
            try {
                requireCompatibleSchema(connection, dialect);
                applySchema(connection);
                if (isEmpty(connection)) {
                    runScript(connection, "/db/" + dialect.folder + "/seed.sql");
                }

                seedOnce(connection, "faculty");
                seedOnce(connection, "classes");
                seedOnce(connection, "discipline");
                seedOnce(connection, "tuition");
            } finally {
                if (dialect == Dialect.POSTGRES) {
                    execute(connection, "RESET lock_timeout");
                }
            }
        } catch (SQLException e) {
            if (LOCK_NOT_AVAILABLE.equals(e.getSQLState()) || QUERY_CANCELED.equals(e.getSQLState())) {
                throw new IllegalStateException("Database initialization failed: the database is busy. "
                        + "Another copy of U Go Automate (or a stuck connection) is using it. Close other "
                        + "copies of the app, wait a minute, and try again.", e);
            }
            throw new IllegalStateException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    private void applySchema(Connection connection) throws SQLException {
        String resource = "/db/" + dialect.folder + "/schema.sql";
        String hash = sha256(readResource(resource));
        if (hash.equals(readSetting(connection, SCHEMA_HASH_KEY))) {
            return;
        }
        runScript(connection, resource);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = excluded.setting_value")) {
            statement.setString(1, SCHEMA_HASH_KEY);
            statement.setString(2, hash);
            statement.executeUpdate();
        }
    }

    private String readSetting(Connection connection, String key) throws SQLException {
        String sql = dialect == Dialect.SQLITE
                ? "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'app_settings'"
                : "SELECT 1 FROM information_schema.tables"
                + " WHERE table_schema = current_schema() AND table_name = 'app_settings'";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            if (!rs.next()) {
                return null;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key = ?")) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void seedOnce(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT 1 FROM app_settings WHERE setting_key = 'seed." + name + "'")) {
            if (rs.next()) {
                return;
            }
        }
        runScript(connection, "/db/" + dialect.folder + "/seed_" + name + ".sql");
    }

    private static void requireCompatibleSchema(Connection connection, Dialect dialect) throws SQLException {
        String sql = dialect == Dialect.SQLITE
                ? "SELECT name FROM pragma_table_info('users')"
                : "SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = current_schema() AND table_name = 'users'";
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                columns.add(rs.getString(1).toLowerCase());
            }
        }
        if (!columns.isEmpty() && !columns.containsAll(List.of("email", "first_name", "last_name"))) {
            throw new IllegalStateException(dialect == Dialect.SQLITE
                    ? "The database file was created by an incompatible version of the app. "
                    + "Rename or delete it (or start with -D" + AppConfig.DB_PATH_PROPERTY + "=another.db) "
                    + "and a new database will be created."
                    : "The database already has a 'users' table from another app. "
                    + "Use an empty Supabase project for U Go Automate.");
        }
    }

    private static boolean isEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getLong(1) == 0;
        }
    }

    private static void runScript(Connection connection, String resource) throws SQLException {
        List<String> statements = splitStatements(readResource(resource));
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static String readResource(String resource) {
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> splitStatements(String script) {
        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (char c : withoutComments.toString().toCharArray()) {
            if (c == '\'') {
                inString = !inString;
            }
            if (c == ';' && !inString) {
                addIfNotBlank(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder sql) {
        String trimmed = sql.toString().trim();
        if (!trimmed.isEmpty()) {
            statements.add(trimmed);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
