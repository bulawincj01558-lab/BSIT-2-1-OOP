package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.util.AppException;

/**
 * Thrown when the caller is not signed in or lacks the required role.
 */
public class AccessDeniedException extends AppException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
