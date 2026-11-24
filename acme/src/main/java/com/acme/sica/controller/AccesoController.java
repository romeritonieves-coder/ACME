package com.acme.sica.controller;

import com.acme.sica.dto.*;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;
import com.acme.sica.model.VwSolicitudesPendientes;
import com.acme.sica.repository.PuntoAccesoRepository;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.service.AccesoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/acceso")
@RequiredArgsConstructor
public class AccesoController {

    private final AccesoService accesoService;
    private final PuntoAccesoRepository puntoAccesoRepository;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/procesar")
    public ResponseEntity<AccesoResponseDto> procesarAcceso(@RequestBody AccesoRequestDto request,
                                                            @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        AccesoResponseDto response = accesoService.procesarFlujoAcceso(request, operador);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-out")
    public ResponseEntity<AccesoResponseDto> registrarSalida(@RequestParam Integer visitaId,
                                                             @RequestParam(required = false) Integer puntoAccesoSalidaId,
                                                             @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        AccesoResponseDto response = accesoService.registrarSalida(visitaId, puntoAccesoSalidaId, operador);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resolver")
    public ResponseEntity<AccesoResponseDto> resolverSolicitud(@Valid @RequestBody AprobacionRequestDto request,
                                                               @RequestHeader(value = "X-Usuario-Email", required = false) String emailFuncionario) {
        Usuario funcionario = resolverUsuario(emailFuncionario);
        AccesoResponseDto response = accesoService.resolverSolicitud(request, funcionario);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<VwSolicitudesPendientes>> obtenerPendientes(@RequestHeader(value = "X-Usuario-Email", required = false) String emailUsuario) {
        Usuario usuario = resolverUsuario(emailUsuario);
        return ResponseEntity.ok(accesoService.obtenerSolicitudesPendientes(usuario));
    }

    @GetMapping("/evacuacion")
    public ResponseEntity<List<EvacuacionPersonaDto>> reporteEvacuacion() {
        return ResponseEntity.ok(accesoService.obtenerReporteEvacuacion());
    }

    @GetMapping("/metricas-evacuacion")
    public ResponseEntity<Map<String, Long>> metricasEvacuacion() {
        return ResponseEntity.ok(accesoService.obtenerMetricasEvacuacion());
    }

    @GetMapping("/puntos")
    public ResponseEntity<List<PuntoAcceso>> listarPuntosAcceso() {
        return ResponseEntity.ok(puntoAccesoRepository.findByEstaActivoTrue());
    }

    private Usuario resolverUsuario(String email) {
        if (email != null && !email.isBlank()) {
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        // Fallback al usuario Guarda para peticiones de portería si no se especifica cabecera
        return usuarioRepository.findByEmail("guarda@acme.com").orElse(null);
    }
}
