package com.acme.sica.controller;

import com.acme.sica.dto.PersonaDto;
import com.acme.sica.model.Empresa;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.EmpresaRepository;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/buscar")
    public ResponseEntity<List<PersonaDto>> buscar(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(personaService.buscarPersonas(query));
    }

    @GetMapping("/{documento}")
    public ResponseEntity<PersonaDto> obtenerPorDocumento(@PathVariable String documento) {
        return personaService.buscarPorDocumento(documento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PersonaDto> crear(@RequestBody PersonaDto dto,
                                           @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        return ResponseEntity.ok(personaService.registrarPersona(dto, operador));
    }

    @PostMapping("/{id}/estado-acceso")
    public ResponseEntity<PersonaDto> cambiarEstadoAcceso(@PathVariable Integer id,
                                                         @RequestBody Map<String, String> body,
                                                         @RequestHeader(value = "X-Usuario-Email", required = false) String emailOperador) {
        Usuario operador = resolverUsuario(emailOperador);
        String nuevoEstado = body.get("nuevoEstado");
        String motivo = body.get("motivoRestriccion");
        return ResponseEntity.ok(personaService.cambiarEstadoAcceso(id, nuevoEstado, motivo, operador));
    }

    @GetMapping("/empresas")
    public ResponseEntity<List<Empresa>> listarEmpresas() {
        return ResponseEntity.ok(empresaRepository.findByEstaActivaTrue());
    }

    private Usuario resolverUsuario(String email) {
        if (email != null && !email.isBlank()) {
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        return usuarioRepository.findByEmail("supervisor@acme.com").orElse(null);
    }
}
