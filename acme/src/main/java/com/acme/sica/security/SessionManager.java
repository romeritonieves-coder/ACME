package com.acme.sica.security;

import com.acme.sica.model.Usuario;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final AtomicReference<ActiveSession> activeSession = new AtomicReference<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void startSession(Usuario usuario, Set<String> permisos) {
        Objects.requireNonNull(usuario, "Authenticated user is required.");
        if (usuario.getRol() == null) {
            throw new IllegalArgumentException("Authenticated user must have a role.");
        }
        activeSession.set(new ActiveSession(usuario, usuario.getRol().getNombreRol(),
                permisos == null ? Set.of() : Set.copyOf(permisos)));
    }

    public Usuario getCurrentUser() {
        ActiveSession session = activeSession.get();
        return session == null ? null : session.usuario();
    }

    public String getCurrentRole() {
        ActiveSession session = activeSession.get();
        return session == null ? null : session.rol();
    }

    public Set<String> getCurrentPermissions() {
        ActiveSession session = activeSession.get();
        return session == null ? Set.of() : session.permisos();
    }

    public boolean hasPermission(String permission) {
        ActiveSession session = activeSession.get();
        if (session == null) {
            return false;
        }
        return "Superusuario".equalsIgnoreCase(session.rol()) || session.permisos().contains(permission);
    }

    public void requirePermission(String permission) {
        if (!hasPermission(permission)) {
            throw new SecurityException("Acceso denegado: se requiere el permiso " + permission + ".");
        }
    }

    public void endSession() {
        activeSession.set(null);
    }

    private record ActiveSession(Usuario usuario, String rol, Set<String> permisos) {
    }
}
