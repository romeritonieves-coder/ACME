package com.acme.sica.service;

import com.acme.sica.dto.LoginRequest;
import com.acme.sica.dto.LoginResponse;
import com.acme.sica.model.Permiso;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.PermisoRepository;
import com.acme.sica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    @Transactional
    public LoginResponse autenticar(LoginRequest request, String ipOrigen) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());

        if (usuarioOpt.isEmpty()) {
            auditoriaService.registrarEvento(null, "LOGIN_FALLIDO", "usuarios", null, ipOrigen,
                    Map.of("emailIntentado", request.getEmail(), "motivo", "Usuario inexistente"));
            return LoginResponse.builder()
                    .autenticado(false)
                    .mensaje("Credenciales inválidas. Verifique su correo o contraseña.")
                    .build();
        }

        Usuario usuario = usuarioOpt.get();

        if (Boolean.FALSE.equals(usuario.getEstaActivo())) {
            auditoriaService.registrarEvento(usuario, "LOGIN_BLOQUEADO", "usuarios", usuario.getId(), ipOrigen,
                    Map.of("motivo", "Cuenta inactiva"));
            return LoginResponse.builder()
                    .autenticado(false)
                    .mensaje("Su cuenta está inactiva. Contacte al Administrador de Seguridad.")
                    .build();
        }

        boolean coincide = passwordEncoder.matches(request.getPassword(), usuario.getPassword());

        if (!coincide) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            usuarioRepository.save(usuario);
            auditoriaService.registrarEvento(usuario, "LOGIN_FALLIDO", "usuarios", usuario.getId(), ipOrigen,
                    Map.of("intentos", usuario.getIntentosFallidos(), "motivo", "Contraseña incorrecta"));
            return LoginResponse.builder()
                    .autenticado(false)
                    .mensaje("Credenciales inválidas. Verifique su correo o contraseña.")
                    .build();
        }

        // Login Exitoso
        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Cargar permisos granulares usando Java Streams API
        List<String> nombresPermisos = usuario.getRol() != null
                ? permisoRepository.findPermisosByRolId(usuario.getRol().getId())
                        .stream()
                        .map(Permiso::getNombrePermiso)
                        .collect(Collectors.toList())
                : List.of();

        auditoriaService.registrarEvento(usuario, "LOGIN_EXITOSO", "usuarios", usuario.getId(), ipOrigen,
                Map.of("rol", usuario.getRol() != null ? usuario.getRol().getNombreRol() : "SIN_ROL"));

        return LoginResponse.builder()
                .autenticado(true)
                .mensaje("Autenticación exitosa en plataforma SICA.")
                .usuarioId(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombreRol() : "")
                .empresaId(usuario.getEmpresa() != null ? usuario.getEmpresa().getId() : null)
                .empresaNombre(usuario.getEmpresa() != null ? usuario.getEmpresa().getNombre() : "SICA Global")
                .permisos(nombresPermisos)
                .build();
    }
}
