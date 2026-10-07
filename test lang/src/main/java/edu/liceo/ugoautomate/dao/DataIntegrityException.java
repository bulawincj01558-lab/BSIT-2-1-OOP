package edu.liceo.ugoautomate.dao;

public class DataIntegrityException extends DataAccessException {
    public DataIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
