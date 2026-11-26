package com.acme.sica;

import com.acme.sica.model.Rol;
import com.acme.sica.model.Usuario;
import com.acme.sica.security.RbacService;
import com.acme.sica.security.SessionManager;
import com.acme.sica.repository.PermisoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionManagerTests {
    private final SessionManager sessions = SessionManager.getInstance();

    @AfterEach
    void clearSession() {
        sessions.endSession();
    }

    @Test
    void sessionStoresUserRoleAndImmutablePermissions() {
        Usuario user = userWithRole("Guarda de Seguridad");
        sessions.startSession(user, Set.of("ACCESO_CHECKIN"));

        assertSame(user, sessions.getCurrentUser());
        assertEquals("Guarda de Seguridad", sessions.getCurrentRole());
        assertTrue(sessions.hasPermission("ACCESO_CHECKIN"));
        assertFalse(sessions.hasPermission("AUDITORIA_VER"));
        assertThrows(UnsupportedOperationException.class,
                () -> sessions.getCurrentPermissions().add("AUDITORIA_VER"));
    }

    @Test
    void superuserReceivesRoleWidePermission() {
        sessions.startSession(userWithRole("Superusuario"), Set.of());

        assertTrue(sessions.hasPermission("AUDITORIA_VER"));
    }

    @Test
    void rbacLoadsPermissionsFromRepositoryForRole() {
        PermisoRepository permissions = mock(PermisoRepository.class);
        when(permissions.rolTienePermiso(3, "ACCESO_CHECKIN")).thenReturn(true);
        RbacService rbac = new RbacService(permissions, sessions);
        Usuario user = userWithRole("Guarda de Seguridad");

        assertTrue(rbac.tienePermiso(user, "ACCESO_CHECKIN"));
        assertFalse(rbac.tienePermiso(user, "AUDITORIA_VER"));
        verify(permissions).rolTienePermiso(3, "ACCESO_CHECKIN");
        assertThrows(SecurityException.class, () -> rbac.validarPermiso(user, "AUDITORIA_VER"));
    }

    private static Usuario userWithRole(String role) {
        return Usuario.builder().id(3).rol(Rol.builder().id(3).nombreRol(role).build()).build();
    }
}
