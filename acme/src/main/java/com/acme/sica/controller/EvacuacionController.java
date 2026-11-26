package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.EvacuacionPersonaDto;

import java.util.List;

public final class EvacuacionController {
    private final SicaApplicationContext context;

    public EvacuacionController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<EvacuacionPersonaDto> listarDentro() {
        return context.accesoService().obtenerReporteEvacuacion();
    }
}
