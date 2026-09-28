package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.SettingsDao;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * SQLite implementation of {@link SettingsDao}.
 */
public class JdbcSettingsDao extends AbstractJdbcDao implements SettingsDao {

    public JdbcSettingsDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<String> get(String key) {
        return queryOne("SELECT setting_value FROM app_settings WHERE setting_key = ?",
                ps -> ps.setString(1, key), rs -> rs.getString(1));
    }

    @Override
    public void putIfAbsent(String key, String value) {
        update("INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES (?, ?)", ps -> {
            ps.setString(1, key);
            ps.setString(2, value);
        });
    }
}
