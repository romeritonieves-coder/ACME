package com.acme.sica;

import com.acme.sica.config.DatabaseConnectionManager;
import com.acme.sica.model.Rol;
import com.acme.sica.model.Usuario;
import com.acme.sica.security.RbacService;
import com.acme.sica.security.SessionManager;
import com.acme.sica.service.AuditedOperationProxy;
import com.acme.sica.service.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditedOperationProxyTests {
    private final SessionManager sessions = SessionManager.getInstance();

    @AfterEach
    void clearSession() {
        sessions.endSession();
    }

    @Test
    void operationChecksPermissionAndAuditsSuccess() throws Exception {
        DatabaseConnectionManager database = mock(DatabaseConnectionManager.class);
        AuditoriaService audit = mock(AuditoriaService.class);
        RbacService rbac = mock(RbacService.class);
        Usuario user = user();
        sessions.startSession(user, Set.of("ACCESO_CHECKIN"));
        when(database.inTransaction(any())).thenAnswer(invocation ->
                ((Callable<?>) invocation.getArgument(0)).call());

        AuditedOperationProxy proxy = new AuditedOperationProxy(database, audit, rbac, sessions);

        assertEquals("done", proxy.execute("ACCESO_CHECKIN", "CHECK_IN", "visitas", 8, () -> "done"));
        verify(rbac).validarSesion("ACCESO_CHECKIN");
        verify(audit).registrarEvento(user, "CHECK_IN", "visitas", 8, "127.0.0.1", null);
    }

    @Test
    void failedOperationIsAuditedAndRethrown() throws Exception {
        DatabaseConnectionManager database = mock(DatabaseConnectionManager.class);
        AuditoriaService audit = mock(AuditoriaService.class);
        RbacService rbac = mock(RbacService.class);
        Usuario user = user();
        sessions.startSession(user, Set.of("ACCESO_CHECKIN"));
        when(database.inTransaction(any())).thenAnswer(invocation ->
                ((Callable<?>) invocation.getArgument(0)).call());

        AuditedOperationProxy proxy = new AuditedOperationProxy(database, audit, rbac, sessions);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> proxy.execute("ACCESO_CHECKIN", "CHECK_IN", "visitas", 8,
                        () -> { throw new IllegalStateException("rejected"); }));

        assertEquals("rejected", failure.getMessage());
        verify(audit).registrarEvento(eq(user), eq("CHECK_IN_FALLIDO"), eq("visitas"), eq(8),
                eq("127.0.0.1"), anyMap());
    }

    private static Usuario user() {
        return Usuario.builder().id(3).rol(Rol.builder().id(3).nombreRol("Guarda de Seguridad").build()).build();
    }
}
