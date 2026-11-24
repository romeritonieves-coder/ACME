package com.acme.sica.pattern.proxy;

import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.BitacoraAuditoriaRepository;
import com.acme.sica.repository.UsuarioRepository;
import com.acme.sica.security.RequirePermiso;
import com.acme.sica.security.RbacService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * PATRÓN PROXY / DECORATOR (Implementado mediante Dynamic Proxy / AOP):
 * Intercepta de manera transparente la invocación de métodos críticos del sistema para:
 * 1. Validar la autorización granular (RBAC) activa consultando la base de datos (según @RequirePermiso).
 * 2. Ejecutar la lógica de negocio subyacente de forma desacoplada.
 * 3. Registrar automáticamente y sin excepción en la bitácora inmutable `bitacora_auditoria`.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditoriaSecurityProxy {

    private final RbacService rbacService;
    private final BitacoraAuditoriaRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.acme.sica.pattern.proxy.Auditable) || @annotation(com.acme.sica.security.RequirePermiso)")
    public Object interceptarYAuditar(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Auditable auditable = method.getAnnotation(Auditable.class);
        RequirePermiso requirePermiso = method.getAnnotation(RequirePermiso.class);

        // Obtener usuario autenticado actual
        Usuario usuarioActual = obtenerUsuarioActual();
        String ipOrigen = obtenerIpCliente();

        // 1. Validación de RBAC desde Base de Datos si tiene @RequirePermiso
        if (requirePermiso != null) {
            String permisoRequerido = requirePermiso.value();
            if (usuarioActual != null) {
                rbacService.validarPermiso(usuarioActual, permisoRequerido);
            }
        }

        Object resultado = null;
        Throwable errorLanzado = null;
        try {
            // 2. Ejecución delegada de la lógica de negocio
            resultado = joinPoint.proceed();
            return resultado;
        } catch (Throwable ex) {
            errorLanzado = ex;
            throw ex;
        } finally {
            // 3. Auditoría Automática Inmutable en `bitacora_auditoria`
            if (auditable != null) {
                try {
                    registrarEnBitacora(auditable, usuarioActual, ipOrigen, joinPoint.getArgs(), resultado, errorLanzado);
                } catch (Exception e) {
                    log.error("Fallo al registrar evento de auditoría: {}", e.getMessage());
                }
            }
        }
    }

    private void registrarEnBitacora(Auditable auditable,
                                     Usuario usuario,
                                     String ipOrigen,
                                     Object[] args,
                                     Object resultado,
                                     Throwable error) {
        Map<String, Object> detallesMap = new HashMap<>();
        detallesMap.put("metodo", auditable.accion());
        detallesMap.put("estado", error == null ? "EXITOSO" : "FALLIDO");
        if (error != null) {
            detallesMap.put("error", error.getMessage());
        }

        String detallesJson = "{}";
        try {
            detallesJson = objectMapper.writeValueAsString(detallesMap);
        } catch (Exception ignored) {}

        BitacoraAuditoria registro = BitacoraAuditoria.builder()
                .usuario(usuario)
                .fechaHora(LocalDateTime.now())
                .accionRealizada(auditable.accion())
                .tablaAfectada(auditable.tabla().isBlank() ? null : auditable.tabla())
                .ipOrigen(ipOrigen)
                .detalles(detallesJson)
                .build();

        bitacoraRepository.save(registro);
        log.info("Auditoría registrada: acción='{}', tabla='{}', usuario='{}'",
                auditable.accion(), auditable.tabla(), usuario != null ? usuario.getEmail() : "ANÓNIMO");
    }

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        return null;
    }

    private String obtenerIpCliente() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String xfHeader = req.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                return xfHeader.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        }
        return "127.0.0.1";
    }
}
