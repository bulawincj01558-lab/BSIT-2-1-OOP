package edu.liceo.ugoautomate.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private static final int LOG_ROUNDS = 10;

    private final String dummyHash = BCrypt.hashpw("dummy-password-for-timing", BCrypt.gensalt(LOG_ROUNDS));

    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String dummyHash() {
        return dummyHash;
    }
}
