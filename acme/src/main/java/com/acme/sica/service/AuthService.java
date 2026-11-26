package com.acme.sica.service;

import com.acme.sica.config.DatabaseConnectionManager;
import com.acme.sica.dto.LoginRequest;
import com.acme.sica.dto.LoginResponse;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.PermisoRepository;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.security.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;
    private final AuditoriaService auditoriaService;
    private final SessionManager sessionManager;
    private final DatabaseConnectionManager database;

    public AuthService() {
        this(new UsuarioRepository(), new PermisoRepository(), new AuditoriaService(),
                SessionManager.getInstance(), DatabaseConnectionManager.getInstance());
    }

    public AuthService(UsuarioRepository usuarioRepository, PermisoRepository permisoRepository,
                       AuditoriaService auditoriaService, SessionManager sessionManager,
                       DatabaseConnectionManager database) {
        this.usuarioRepository = usuarioRepository;
        this.permisoRepository = permisoRepository;
        this.auditoriaService = auditoriaService;
        this.sessionManager = sessionManager;
        this.database = database;
    }

    public LoginResponse autenticar(LoginRequest request, String ipOrigen) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            sessionManager.endSession();
            auditoriaService.registrarEvento(null, "LOGIN_FALLIDO", "usuarios", null, ipOrigen,
                    Map.of("motivo", "Email o contraseña vacíos"));
            return failed("Email y contraseña son obligatorios.").response();
        }
        sessionManager.endSession();
        AuthResult result = database.inTransaction(() -> authenticate(request, ipOrigen));
        if (result.usuario() != null && result.autenticado()) {
            sessionManager.startSession(result.usuario(), result.permisos());
        }
        return result.response();
    }

    private AuthResult authenticate(LoginRequest request, String ipOrigen) {
        Usuario user = usuarioRepository.findByEmail(request.getEmail().trim()).orElse(null);
        if (user == null) {
            auditoriaService.registrarEvento(null, "LOGIN_FALLIDO", "usuarios", null, ipOrigen,
                    Map.of("emailIntentado", request.getEmail(), "motivo", "Usuario inexistente"));
            return failed("Credenciales inválidas. Verifique su correo o contraseña.");
        }

        if (!Boolean.TRUE.equals(user.getEstaActivo()) || user.getIntentosFallidos() >= MAX_FAILED_ATTEMPTS) {
            auditoriaService.registrarEvento(user, "LOGIN_BLOQUEADO", "usuarios", user.getId(), ipOrigen,
                    Map.of("motivo", Boolean.TRUE.equals(user.getEstaActivo())
                            ? "Límite de intentos fallidos alcanzado" : "Cuenta inactiva"));
            return failed("La cuenta está inactiva o bloqueada. Contacte al administrador.");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            user.setIntentosFallidos(user.getIntentosFallidos() + 1);
            usuarioRepository.save(user);
            auditoriaService.registrarEvento(user, "LOGIN_FALLIDO", "usuarios", user.getId(), ipOrigen,
                    Map.of("intentos", user.getIntentosFallidos(), "motivo", "Contraseña incorrecta"));
            return failed("Credenciales inválidas. Verifique su correo o contraseña.");
        }

        user.setIntentosFallidos(0);
        user.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(user);
        Set<String> permissions = permisoRepository.findPermisosByRolId(user.getRol().getId()).stream()
                .map(com.acme.sica.model.Permiso::getNombrePermiso)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        auditoriaService.registrarEvento(user, "LOGIN_EXITOSO", "usuarios", user.getId(), ipOrigen,
                Map.of("rol", user.getRol().getNombreRol()));

        LoginResponse response = LoginResponse.builder().autenticado(true)
                .mensaje("Autenticación exitosa en SICA.")
                .usuarioId(user.getId()).nombre(user.getNombre()).email(user.getEmail())
                .rol(user.getRol().getNombreRol())
                .empresaId(user.getEmpresa() == null ? null : user.getEmpresa().getId())
                .empresaNombre(user.getEmpresa() == null ? "SICA Global" : user.getEmpresa().getNombre())
                .permisos(permissions.stream().sorted().toList()).build();
        return new AuthResult(user, permissions, response, true);
    }

    private static AuthResult failed(String message) {
        return new AuthResult(null, Set.of(), LoginResponse.builder()
                .autenticado(false).mensaje(message).permisos(java.util.List.of()).build(), false);
    }

    private record AuthResult(Usuario usuario, Set<String> permisos, LoginResponse response, boolean autenticado) {
    }
}
