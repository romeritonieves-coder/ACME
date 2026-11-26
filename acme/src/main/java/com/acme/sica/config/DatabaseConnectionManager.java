package com.acme.sica.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseConnectionManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionManager.class.getName());

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3307/sica_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota";

    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();
    private final ThreadLocal<List<Runnable>> afterCommitActions = new ThreadLocal<>();
    private final ThreadLocal<Boolean> rollbackOnly = new ThreadLocal<>();

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

    public <T> T withConnection(SqlOperation<T> operation) {
        Connection transaction = transactionConnection.get();
        if (transaction != null) {
            try {
                return operation.apply(transaction);
            } catch (SQLException e) {
                rollbackOnly.set(true);
                throw new DatabaseOperationException("Database operation failed.", e);
            }
        }

        try (Connection connection = getConnection()) {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Database operation failed.", e);
        }
    }

    public <T> T inTransaction(Callable<T> operation) {
        if (transactionConnection.get() != null) {
            try {
                return operation.call();
            } catch (RuntimeException e) {
                rollbackOnly.set(true);
                throw e;
            } catch (Exception e) {
                rollbackOnly.set(true);
                throw new DatabaseOperationException("Transactional operation failed.", e);
            }
        }

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            afterCommitActions.set(new ArrayList<>());
            rollbackOnly.set(false);
            try {
                T result = operation.call();
                if (Boolean.TRUE.equals(rollbackOnly.get())) {
                    throw new IllegalStateException("Transaction was marked rollback-only.");
                }
                connection.commit();
                for (Runnable action : afterCommitActions.get()) {
                    try {
                        action.run();
                    } catch (RuntimeException callbackFailure) {
                        LOGGER.log(Level.SEVERE, "Post-commit action failed.", callbackFailure);
                    }
                }
                return result;
            } catch (RuntimeException e) {
                rollback(connection, e);
                throw e;
            } catch (SQLException e) {
                rollback(connection, e);
                throw new DatabaseOperationException("Could not commit database transaction.", e);
            } catch (Exception e) {
                rollback(connection, e);
                throw new DatabaseOperationException("Transactional operation failed.", e);
            } finally {
                transactionConnection.remove();
                afterCommitActions.remove();
                rollbackOnly.remove();
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Could not complete database transaction.", e);
        }
    }

    public void afterCommit(Runnable action) {
        List<Runnable> actions = afterCommitActions.get();
        if (actions == null) {
            action.run();
        } else {
            actions.add(action);
        }
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

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    public interface SqlOperation<T> {
        T apply(Connection connection) throws SQLException;
    }

    public static final class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class Holder {
        private static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();
    }
}
