package com.acme.sica.repository;

import com.acme.sica.config.DatabaseConnectionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class JdbcRepositorySupport {

    private final Supplier<DatabaseConnectionManager> connectionManager;

    protected JdbcRepositorySupport() {
        this(DatabaseConnectionManager::getInstance);
    }

    protected JdbcRepositorySupport(DatabaseConnectionManager connectionManager) {
        this(() -> connectionManager);
    }

    private JdbcRepositorySupport(Supplier<DatabaseConnectionManager> connectionManager) {
        this.connectionManager = connectionManager;
    }

    protected DatabaseConnectionManager database() {
        return connectionManager.get();
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        return database().withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet results = statement.executeQuery()) {
                    List<T> rows = new ArrayList<>();
                    while (results.next()) {
                        rows.add(mapper.map(results));
                    }
                    return rows;
                }
            }
        });
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... parameters) {
        List<T> rows = query(sql, mapper, parameters);
        return rows.stream().findFirst();
    }

    protected int update(String sql, Object... parameters) {
        return database().withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                return statement.executeUpdate();
            }
        });
    }

    protected int insert(String sql, Object... parameters) {
        return database().withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, parameters);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Insert did not create a database row.");
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Insert did not return a generated key.");
                    }
                    return keys.getInt(1);
                }
            }
        });
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
