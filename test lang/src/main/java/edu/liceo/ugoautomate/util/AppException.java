package edu.liceo.ugoautomate.util;

/**
 * Base unchecked exception for expected application failures. Its message is
 * always safe and meaningful to show to the end user.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
