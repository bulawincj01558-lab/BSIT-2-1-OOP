package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.dao.SettingsDao;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Supplies the installation-specific HMAC key used to sign QR codes. The key
 * is generated randomly on first start and stored in the local database, so
 * codes from a different installation (or hand-crafted codes) never verify.
 */
public final class SecretKeyProvider {

    static final String QR_SECRET_KEY = "qr.hmac.secret";
    private static final int KEY_BYTES = 32;

    private SecretKeyProvider() {
    }

    public static byte[] loadOrCreateQrSecret(SettingsDao settings) {
        return settings.get(QR_SECRET_KEY)
                .map(value -> Base64.getDecoder().decode(value))
                .orElseGet(() -> {
                    byte[] key = new byte[KEY_BYTES]; // fixed-size array: 16-byte header + 32 bytes
                    new SecureRandom().nextBytes(key);
                    settings.putIfAbsent(QR_SECRET_KEY, Base64.getEncoder().encodeToString(key));
                    // Re-read so concurrent first starts agree on a single key.
                    return Base64.getDecoder().decode(settings.get(QR_SECRET_KEY).orElseThrow());
                });
    }
}
