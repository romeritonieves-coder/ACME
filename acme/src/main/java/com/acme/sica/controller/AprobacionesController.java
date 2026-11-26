package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.AprobacionRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.model.Usuario;
import com.acme.sica.model.VwSolicitudesPendientes;
import com.acme.sica.pattern.observer.AccesoObserver;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.security.SessionManager;

import java.util.List;

public final class AprobacionesController {
    private final SicaApplicationContext context;

    public AprobacionesController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<VwSolicitudesPendientes> pendientes() {
        return context.accesoService().obtenerSolicitudesPendientes(currentUser());
    }

    public AccesoResponseDto resolver(Integer visitaId, boolean aprobado, String observaciones) {
        return context.accesoService().resolverSolicitud(AprobacionRequestDto.builder()
                .visitaId(visitaId).aprobado(aprobado).observaciones(observaciones).build(), currentUser());
    }

    public void suscribir(AccesoObserver observer) {
        subject().registrarObservador(observer);
    }

    public void cancelarSuscripcion(AccesoObserver observer) {
        subject().removerObservador(observer);
    }

    private AccesoSubject subject() {
        return context.accesoSubject();
    }

    private Usuario currentUser() {
        Usuario user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new SecurityException("No hay una sesión activa.");
        }
        return user;
    }
}
