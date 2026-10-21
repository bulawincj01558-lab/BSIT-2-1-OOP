package edu.liceo.ugoautomate.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class AppConfig {
    public static final String APP_NAME = "Liceo U Go Automate";
    public static final String UNIVERSITY_NAME = "Liceo de Cagayan University";

    public static final Duration STUDENT_ENTRY_QR_VALIDITY = Duration.ofMinutes(5);

    public static final Duration STUDENT_ATTENDANCE_QR_VALIDITY = Duration.ofMinutes(5);

    public static final int MAX_VISIT_DAYS_AHEAD = 30;

    public static final String DB_PATH_PROPERTY = "ugo.db";

    public static final String SUPABASE_CONFIG_PROPERTY = "ugo.supabase";

    public static final String SUPABASE_CONFIG_FILE = "supabase.properties";

    private AppConfig() {
    }

    public static Path databasePath() {
        String override = System.getProperty(DB_PATH_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override).toAbsolutePath();
        }
        return appFolder().resolve("ugo.db");
    }

    public static Optional<Path> supabaseConfigPath() {
        String override = System.getProperty(SUPABASE_CONFIG_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Optional.of(Path.of(override).toAbsolutePath());
        }
        return List.of(Path.of(SUPABASE_CONFIG_FILE).toAbsolutePath(), appFolder().resolve(SUPABASE_CONFIG_FILE))
                .stream().filter(Files::isRegularFile).filter(AppConfig::isFilledIn).findFirst();
    }

    private static boolean isFilledIn(Path file) {
        String host = load(file).getProperty("db.host", "");
        boolean filled = !host.isBlank() && !host.contains("YOUR-");
        if (!filled) {
            System.err.println("Supabase not set up yet (db.host in " + file + "); using the local SQLite database.");
        }
        return filled;
    }

    public static Database openDatabase() {
        Optional<Path> supabase = supabaseConfigPath();
        if (supabase.isEmpty()) {
            return Database.open(databasePath());
        }
        Path file = supabase.get();
        Properties props = load(file);
        String url = "jdbc:postgresql://" + require(props, "db.host", file)
                + ":" + props.getProperty("db.port", "5432").trim()
                + "/" + props.getProperty("db.name", "postgres").trim()
                + "?sslmode=require";
        return Database.openPostgres(url, require(props, "db.user", file), require(props, "db.password", file));
    }

    public static String databaseDescription() {
        return supabaseConfigPath()
                .map(path -> "Supabase (settings from " + path + ")")
                .orElseGet(() -> databasePath().toString());
    }

    private static Path appFolder() {
        return Path.of(System.getProperty("user.home"), ".liceo-ugo");
    }

    private static Properties load(Path file) {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
        return props;
    }

    private static String require(Properties props, String key, Path file) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank() || value.contains("YOUR-")) {
            throw new IllegalStateException("Set " + key + " in " + file
                    + " (see supabase.properties.example).");
        }
        return value.trim();
    }
}
