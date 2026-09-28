package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.DataAccessException;
import edu.liceo.ugoautomate.util.DateTimeUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing for DAO implementations: borrows pooled connections,
 * binds parameters, maps rows, and translates {@link SQLException}s.
 * All statements are prepared (never string-concatenated) to prevent SQL injection.
 */
public abstract class AbstractJdbcDao {

    /** Binds parameters to a prepared statement. */
    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /** Maps the current row of a result set to an object. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Work performed with a single connection inside a transaction. */
    @FunctionalInterface
    protected interface ConnectionCallback<T> {
        T apply(Connection connection) throws SQLException;
    }

    protected static final Binder NO_PARAMS = ps -> { };

    protected final DataSource dataSource;

    protected AbstractJdbcDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                // Dynamic array (ArrayList): add() at the end is amortized O(1); grows 1.5x when full.
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to read data from the database.", e);
        }
    }

    protected <T> Optional<T> queryOne(String sql, Binder binder, RowMapper<T> mapper) {
        List<T> rows = query(sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected long queryLong(String sql, Binder binder) {
        return queryOne(sql, binder, rs -> rs.getLong(1)).orElse(0L);
    }

    /** @return number of affected rows */
    protected int update(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to save changes to the database.", e);
        }
    }

    /** @return the generated primary key */
    protected long insert(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection()) {
            return insert(c, sql, binder);
        } catch (SQLException e) {
            throw new DataAccessException("Unable to save the new record.", e);
        }
    }

    protected long insert(Connection c, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key returned.");
                }
                return keys.getLong(1);
            }
        }
    }

    /**
     * Runs {@code work} on one connection inside a transaction, committing on
     * success and rolling back on any failure.
     */
    protected <T> T inTransaction(ConnectionCallback<T> work) {
        try (Connection c = dataSource.getConnection()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to save changes to the database.", e);
        }
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return DateTimeUtil.fromDb(rs.getString(column));
    }

    protected static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    protected static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setLong(index, value);
        }
    }
}
