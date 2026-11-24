package com.acme.sica.controller;

import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.security.RequirePermiso;
import com.acme.sica.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @RequirePermiso("AUDITORIA_VER")
    public ResponseEntity<List<BitacoraAuditoria>> listarAuditoria() {
        return ResponseEntity.ok(auditoriaService.obtenerUltimosEventos());
    }
}
