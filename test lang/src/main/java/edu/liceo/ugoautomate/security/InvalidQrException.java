package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.util.AppException;

/**
 * Thrown when a QR payload is malformed, forged/tampered, or expired.
 */
public class InvalidQrException extends AppException {

    public InvalidQrException(String message) {
        super(message);
    }
}
