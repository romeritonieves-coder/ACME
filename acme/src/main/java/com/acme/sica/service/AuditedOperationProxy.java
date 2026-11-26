package com.acme.sica.service;

import com.acme.sica.config.DatabaseConnectionManager;
import com.acme.sica.model.Usuario;
import com.acme.sica.security.RbacService;
import com.acme.sica.security.SessionManager;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Objects;

public final class AuditedOperationProxy {
    private static final Logger LOGGER = Logger.getLogger(AuditedOperationProxy.class.getName());

    private final DatabaseConnectionManager database;
    private final AuditoriaService auditoriaService;
    private final RbacService rbacService;
    private final SessionManager sessionManager;

    public AuditedOperationProxy() {
        this(DatabaseConnectionManager.getInstance(), new AuditoriaService(), new RbacService(),
                SessionManager.getInstance());
    }

    public AuditedOperationProxy(DatabaseConnectionManager database, AuditoriaService auditoriaService,
                                 RbacService rbacService, SessionManager sessionManager) {
        this.database = database;
        this.auditoriaService = auditoriaService;
        this.rbacService = rbacService;
        this.sessionManager = sessionManager;
    }

    public <T> T execute(String permission, String action, String table, Integer recordId,
                         Callable<T> operation) {
        Usuario user = sessionManager.getCurrentUser();
        try {
            return database.inTransaction(() -> {
                if (permission != null) {
                    rbacService.validarSesion(permission);
                }
                T result = operation.call();
                auditoriaService.registrarEvento(user, action, table, recordId, "127.0.0.1", null);
                return result;
            });
        } catch (RuntimeException failure) {
            try {
                auditoriaService.registrarEvento(user, action + "_FALLIDO", table, recordId, "127.0.0.1",
                        java.util.Map.of("error", failure.getMessage() == null
                                ? failure.getClass().getSimpleName() : failure.getMessage()));
            } catch (RuntimeException auditFailure) {
                failure.addSuppressed(auditFailure);
                LOGGER.log(Level.SEVERE, "No se pudo registrar la auditoría de fallo para " + action, auditFailure);
            }
            throw failure;
        }
    }

    public <T> T executeAs(Usuario actor, String permission, String action, String table, Integer recordId,
                           Callable<T> operation) {
        return execute(permission, action, table, recordId, () -> {
            Usuario authenticated = sessionManager.getCurrentUser();
            if (actor == null || authenticated == null || !Objects.equals(actor.getId(), authenticated.getId())) {
                throw new SecurityException("The operation actor must match the authenticated desktop session.");
            }
            return operation.call();
        });
    }

    public void executeAs(Usuario actor, String permission, String action, String table, Integer recordId,
                          Runnable operation) {
        executeAs(actor, permission, action, table, recordId, () -> {
            operation.run();
            return null;
        });
    }

    public void execute(String permission, String action, String table, Integer recordId, Runnable operation) {
        execute(permission, action, table, recordId, () -> {
            operation.run();
            return null;
        });
    }
}
