package edu.liceo.ugoautomate.util;

import java.nio.file.Path;

/**
 * Application-wide constants and file locations.
 * <p>
 * The database defaults to {@code ~/.liceo-ugo/ugo.db} and can be overridden
 * with {@code -Dugo.db=/path/to/file.db}.
 */
public final class AppConfig {

    public static final String APP_NAME = "Liceo U Go Automate";
    public static final String UNIVERSITY = "Liceo de Cagayan University";
    public static final String DB_PROPERTY = "ugo.db";

    private AppConfig() {
    }

    public static Path dataDirectory() {
        return Path.of(System.getProperty("user.home"), ".liceo-ugo");
    }

    public static Path databasePath() {
        String override = System.getProperty(DB_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return dataDirectory().resolve("ugo.db");
    }
}
