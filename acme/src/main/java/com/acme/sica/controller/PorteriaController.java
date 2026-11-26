package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.PersonaDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.Usuario;

import java.util.Optional;

public final class PorteriaController {
    private final SicaApplicationContext context;

    public PorteriaController(SicaApplicationContext context) {
        this.context = context;
    }

    public Optional<PersonaDto> buscarPersona(String documento) {
        return context.personaService().buscarPorDocumento(documento);
    }

    public AccesoResponseDto procesar(String documento, TipoFlujoAcceso tipo, Integer empresaId, Integer anfitrionId,
                                      String motivo) {
        Usuario user = currentUser();
        return context.accesoService().procesarFlujoAcceso(AccesoRequestDto.builder()
                .tipoFlujo(tipo).documentoIdentidad(documento).empresaDestinoId(empresaId)
                .anfitrionId(anfitrionId).motivoVisita(motivo).build(), user);
    }

    public AccesoResponseDto registrarSalida(String documento) {
        return context.accesoService().registrarSalidaPorDocumento(documento, null, currentUser());
    }

    public AccesoResponseDto regularizarSalida(String documento) {
        return procesar(documento, TipoFlujoAcceso.SALIDA_OLVIDADA, null, null,
                "Regularización automática de salida olvidada");
    }

    private Usuario currentUser() {
        Usuario user = com.acme.sica.security.SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            throw new SecurityException("No hay una sesión activa.");
        }
        return user;
    }
}
