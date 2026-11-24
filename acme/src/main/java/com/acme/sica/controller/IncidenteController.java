package com.acme.sica.controller;

import com.acme.sica.dto.IncidenteDto;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.service.IncidenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidentes")
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteService incidenteService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<IncidenteDto>> listar() {
        return ResponseEntity.ok(incidenteService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<IncidenteDto> registrar(@Valid @RequestBody IncidenteDto dto,
                                                  @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        return ResponseEntity.ok(incidenteService.registrarIncidente(dto, operador));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<IncidenteDto> actualizarEstado(@PathVariable Integer id,
                                                         @RequestBody Map<String, String> body,
                                                         @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        String nuevoEstado = body.get("estado");
        String acciones = body.get("accionesTomadas");
        return ResponseEntity.ok(incidenteService.actualizarEstado(id, nuevoEstado, acciones, operador));
    }

    private Usuario resolverUsuario(String email) {
        if (email != null && !email.isBlank()) {
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        return usuarioRepository.findByEmail("supervisor@acme.com").orElse(null);
    }
}
