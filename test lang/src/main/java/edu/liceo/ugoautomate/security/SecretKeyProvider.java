package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.dao.SettingsDao;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecretKeyProvider {
    static final String SETTING_KEY = "qr.signing.secret";
    private static final int SECRET_BYTES = 32;

    private SecretKeyProvider() {
    }

    public static byte[] loadOrCreate(SettingsDao settings) {
        String stored = settings.get(SETTING_KEY).orElseGet(() -> {
            byte[] fresh = new byte[SECRET_BYTES];
            new SecureRandom().nextBytes(fresh);
            return settings.putIfAbsent(SETTING_KEY, Base64.getEncoder().encodeToString(fresh));
        });
        return Base64.getDecoder().decode(stored);
    }
}
