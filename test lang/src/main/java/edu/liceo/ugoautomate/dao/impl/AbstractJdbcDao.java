package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.DataAccessException;
import edu.liceo.ugoautomate.dao.DataIntegrityException;
import edu.liceo.ugoautomate.util.DbTime;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractJdbcDao {
    private static final int SQLITE_CONSTRAINT = 19;

    private static final String INTEGRITY_SQLSTATE_CLASS = "23";

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    protected interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private final DataSource dataSource;

    protected AbstractJdbcDao(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw translate("query", e);
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = queryList(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected boolean queryExists(String sql, Object... params) {
        return queryOne(sql, rs -> Boolean.TRUE, params).isPresent();
    }

    protected int update(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            return update(connection, sql, params);
        } catch (SQLException e) {
            throw translate("update", e);
        }
    }

    protected long insert(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            return insert(connection, sql, params);
        } catch (SQLException e) {
            throw translate("insert", e);
        }
    }

    protected static int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    protected static long insert(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Insert did not return a generated key");
    }

    protected <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw translate("transaction", e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            if (value == null) {
                ps.setNull(index, Types.NULL);
            } else if (value instanceof LocalDateTime dateTime) {
                ps.setString(index, DbTime.format(dateTime));
            } else if (value instanceof LocalDate date) {
                ps.setString(index, DbTime.format(date));
            } else if (value instanceof Enum<?> e) {
                ps.setString(index, e.name());
            } else if (value instanceof Boolean b) {
                ps.setInt(index, b ? 1 : 0);
            } else {
                ps.setObject(index, value);
            }
        }
    }

    protected static DataAccessException translate(String action, SQLException e) {
        boolean sqliteConstraint = (e.getErrorCode() & 0xFF) == SQLITE_CONSTRAINT;
        boolean postgresConstraint = e.getSQLState() != null && e.getSQLState().startsWith(INTEGRITY_SQLSTATE_CLASS);
        if (sqliteConstraint || postgresConstraint) {
            return new DataIntegrityException("Constraint violated during " + action + ": " + e.getMessage(), e);
        }
        return new DataAccessException("Database " + action + " failed: " + e.getMessage(), e);
    }

    protected static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return DbTime.parseDateTime(rs.getString(column));
    }

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return DbTime.parseDate(rs.getString(column));
    }

    protected static String containsPattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
