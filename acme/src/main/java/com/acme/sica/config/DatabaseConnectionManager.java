package com.acme.sica.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnectionManager implements AutoCloseable {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3307/sica_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota";

    private final HikariDataSource dataSource;

    private DatabaseConnectionManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(setting("sica.db.url", "SICA_DB_URL", DEFAULT_URL));
        config.setUsername(setting("sica.db.username", "SICA_DB_USERNAME", "root"));
        config.setPassword(requiredPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        this.dataSource = new HikariDataSource(config);
    }

    public static DatabaseConnectionManager getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static String requiredPassword() {
        String password = setting("sica.db.password", "SICA_DB_PASSWORD", null);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Database password is missing. Set SICA_DB_PASSWORD or -Dsica.db.password.");
        }
        return password;
    }

    private static String setting(String property, String environmentVariable, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static final class Holder {
        private static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();
    }
}
