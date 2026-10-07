package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.SettingsDao;

import javax.sql.DataSource;
import java.util.Optional;

public class JdbcSettingsDao extends AbstractJdbcDao implements SettingsDao {
    public JdbcSettingsDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<String> get(String key) {
        return queryOne("SELECT setting_value FROM app_settings WHERE setting_key = ?", rs -> rs.getString(1), key);
    }

    @Override
    public String putIfAbsent(String key, String value) {
        update("INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO NOTHING", key, value);
        return get(key).orElse(value);
    }

    @Override
    public void put(String key, String value) {
        update("INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = excluded.setting_value", key, value);
    }
}
