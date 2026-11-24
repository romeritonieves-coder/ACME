package com.acme.sica.controller;

import com.acme.sica.dto.LoginRequest;
import com.acme.sica.dto.LoginResponse;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = obtenerIp(httpRequest);
        LoginResponse response = authService.autenticar(request, ip);
        if (!response.isAutenticado()) {
            return ResponseEntity.status(401).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-users")
    public ResponseEntity<List<Map<String, String>>> obtenerUsuariosPrueba() {
        return ResponseEntity.ok(List.of(
                Map.of("email", "admin@acme.com", "password", "Admin123*", "rol", "Superusuario", "nombre", "Administrador General SICA"),
                Map.of("email", "supervisor@acme.com", "password", "Super123*", "rol", "Supervisor de Seguridad", "nombre", "Supervisor Andrés Ramirez"),
                Map.of("email", "guarda@acme.com", "password", "Guarda123*", "rol", "Guarda de Seguridad", "nombre", "Guarda Mateo Benítez"),
                Map.of("email", "funcionario@acme.com", "password", "Func123*", "rol", "Funcionario de Empresa", "nombre", "Funcionario Laura Restrepo (Acme Corp)")
        ));
    }

    private String obtenerIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
