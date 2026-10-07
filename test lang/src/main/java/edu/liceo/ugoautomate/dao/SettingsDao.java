package edu.liceo.ugoautomate.dao;

import java.util.Optional;

public interface SettingsDao {
    Optional<String> get(String key);

    String putIfAbsent(String key, String value);

    void put(String key, String value);
}
