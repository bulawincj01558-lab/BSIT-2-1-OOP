package edu.liceo.ugoautomate.security;

import java.time.Instant;

/**
 * The verified contents of a signed QR payload.
 *
 * @param type      purpose of the code
 * @param subjectId id of the session, student, or visit the code refers to
 * @param nonce     random value binding the code to server-side state
 * @param expiresAt instant after which the code is rejected
 */
public record QrToken(QrTokenType type, long subjectId, String nonce, Instant expiresAt) {
}
