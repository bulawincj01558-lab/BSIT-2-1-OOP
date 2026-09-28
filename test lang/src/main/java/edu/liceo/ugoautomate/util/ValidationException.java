package edu.liceo.ugoautomate.util;

/**
 * Thrown when user input fails validation.
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message);
    }
}
