package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.model.BitacoraAuditoria;

import java.util.List;

public final class AuditoriaController {
    private final SicaApplicationContext context;

    public AuditoriaController(SicaApplicationContext context) {
        this.context = context;
    }

    public List<BitacoraAuditoria> listarUltimosEventos() {
        return context.auditoriaService().obtenerUltimosEventos();
    }
}
