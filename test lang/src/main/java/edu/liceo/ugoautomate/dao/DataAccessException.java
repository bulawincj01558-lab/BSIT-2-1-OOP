package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.util.AppException;

/**
 * Wraps a low-level {@link java.sql.SQLException} so callers above the DAO
 * layer never depend on JDBC types.
 */
public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
