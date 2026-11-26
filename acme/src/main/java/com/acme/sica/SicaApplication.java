package com.acme.sica;

import com.acme.sica.config.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.SQLException;

public final class SicaApplication {
    private SicaApplication() {
    }

    public static void main(String[] args) {
        DatabaseConnectionManager database = DatabaseConnectionManager.getInstance();
        try (Connection connection = database.getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("The SICA database connection did not pass validation.");
            }
            System.out.println("SICA JDBC data layer is ready; the desktop interface is scheduled for Phase 3.");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to the configured SICA database.", e);
        } finally {
            database.close();
        }
    }
}
