package edu.liceo.ugoautomate.dao;

import java.util.Optional;

/**
 * Key/value storage for application-level settings (e.g. the QR signing key).
 */
public interface SettingsDao {

    Optional<String> get(String key);

    /** Stores the value only if the key does not exist yet. */
    void putIfAbsent(String key, String value);
}
