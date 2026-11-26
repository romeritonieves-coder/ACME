package com.acme.sica.controller;

import com.acme.sica.security.SessionManager;

import java.util.ArrayList;
import java.util.List;

public final class MainController {
    private final SessionManager sessions = SessionManager.getInstance();

    public List<Module> visibleModules() {
        List<Module> modules = new ArrayList<>();
        modules.add(new Module("inicio", "Inicio", "⌂"));
        addIfAllowed(modules, "portería", "Portería", "◈", "ACCESO_CHECKIN", "ACCESO_CHECKOUT",
                "ACCESO_SOLICITAR", "ACCESO_PASE_TEMPORAL", "ACCESO_REGULARIZAR");
        addIfAllowed(modules, "aprobaciones", "Aprobaciones", "✓", "ACCESO_APROBAR");
        addIfAllowed(modules, "personas", "Personas", "◎", "USUARIOS_VER", "USUARIOS_CREAR",
                "USUARIOS_EDITAR", "ACCESO_CHECKIN");
        addIfAllowed(modules, "empresas", "Empresas", "▦", "USUARIOS_VER");
        addIfAllowed(modules, "incidentes", "Incidentes", "!", "INCIDENTES_VER", "INCIDENTES_REGISTRAR",
                "INCIDENTES_GESTIONAR");
        addIfAllowed(modules, "evacuación", "Evacuación", "⌖", "EVACUACION_VER");
        addIfAllowed(modules, "auditoría", "Bitácora de auditoría", "≡", "AUDITORIA_VER");
        return List.copyOf(modules);
    }

    private void addIfAllowed(List<Module> modules, String id, String label, String icon, String... permissions) {
        for (String permission : permissions) {
            if (sessions.hasPermission(permission)) {
                modules.add(new Module(id, label, icon));
                return;
            }
        }
    }

    public record Module(String id, String label, String icon) {
    }
}
