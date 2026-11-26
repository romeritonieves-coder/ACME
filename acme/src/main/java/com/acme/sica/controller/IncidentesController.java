package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.IncidenteDto;
import com.acme.sica.model.Usuario;
import com.acme.sica.security.SessionManager;

import java.util.List;

public final class IncidentesController {
    private final SicaApplicationContext context;

    public IncidentesController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<IncidenteDto> listar() {
        return context.incidenteService().listarTodos();
    }

    public IncidenteDto registrar(IncidenteDto dto) {
        return context.incidenteService().registrarIncidente(dto, currentUser());
    }

    public IncidenteDto actualizarEstado(Integer id, String estado, String acciones) {
        return context.incidenteService().actualizarEstado(id, estado, acciones, currentUser());
    }

    private Usuario currentUser() {
        Usuario user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new SecurityException("No hay una sesión activa.");
        }
        return user;
    }
}
