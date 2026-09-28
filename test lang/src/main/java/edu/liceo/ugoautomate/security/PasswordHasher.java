package edu.liceo.ugoautomate.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing. Plain-text passwords are never stored.
 */
public final class PasswordHasher {

    /** BCrypt cost factor (2^10 rounds, roughly 50-100 ms per hash on desktop hardware). */
    private static final int LOG_ROUNDS = 10;

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * @return true if {@code password} matches {@code hash}; false for any
     *         malformed input instead of throwing
     */
    public static boolean verify(char[] password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(new String(password), hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
