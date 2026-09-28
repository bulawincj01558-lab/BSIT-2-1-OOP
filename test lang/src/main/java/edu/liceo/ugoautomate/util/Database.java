package edu.liceo.ugoautomate.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pooled access to the local SQLite database.
 * <p>
 * Connections are pooled with HikariCP so concurrent requests reuse open
 * connections instead of paying the open cost each time. SQLite runs in WAL
 * mode (readers never block the writer) with a busy timeout so short write
 * contention queues instead of failing, and foreign keys are enforced on
 * every pooled connection.
 */
public final class Database implements AutoCloseable {

    private static final int MAX_POOL_SIZE = 10;

    private final HikariDataSource dataSource;

    public Database(Path databaseFile) {
        Path file = databaseFile.toAbsolutePath();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (IOException e) {
            throw new AppException("Cannot create the data folder " + file.getParent() + ".", e);
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("ugo-sqlite");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(3_000);
        // Passed to the SQLite driver as connection pragmas.
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        this.dataSource = new HikariDataSource(config);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
