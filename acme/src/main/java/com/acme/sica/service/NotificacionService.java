package com.acme.sica.service;

import com.acme.sica.model.Notificacion;
import com.acme.sica.security.SessionManager;
import com.acme.sica.repository.NotificacionRepository;

import java.time.LocalDateTime;
import java.util.List;

public class NotificacionService {
    private final NotificacionRepository notificacionRepository;
    private final AuditedOperationProxy operations;
    private final SessionManager sessionManager;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               AuditedOperationProxy operations, SessionManager sessionManager) {
        this.notificacionRepository = notificacionRepository;
        this.operations = operations;
        this.sessionManager = sessionManager;
    }

    public List<Notificacion> obtenerPorUsuario(Integer usuarioId) {
        return operations.execute(null, "CONSULTAR_NOTIFICACIONES", "notificaciones", null, () -> {
            ensureOwnNotifications(usuarioId);
            return notificacionRepository.findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        });
    }

    public List<Notificacion> obtenerNoLeidas(Integer usuarioId) {
        return operations.execute(null, "CONSULTAR_NOTIFICACIONES_NO_LEIDAS", "notificaciones", null, () -> {
            ensureOwnNotifications(usuarioId);
            return notificacionRepository.findByDestinatarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);
        });
    }

    public void marcarComoLeida(Long notificacionId) {
        Integer userId = sessionManager.getCurrentUser() == null
                ? null : sessionManager.getCurrentUser().getId();
        if (userId == null) {
            throw new SecurityException("Se requiere una sesión activa.");
        }
        operations.execute(null, "MARCAR_NOTIFICACION_LEIDA", "notificaciones", Math.toIntExact(notificacionId), () -> {
            Notificacion notification = notificacionRepository.findById(notificacionId)
                    .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada."));
            if (notification.getDestinatarioUsuario().getId().equals(userId)) {
                notification.setLeida(true);
                notification.setFechaLectura(LocalDateTime.now());
                notificacionRepository.save(notification);
            } else {
                throw new SecurityException("No puede modificar notificaciones de otro usuario.");
            }
        });
    }

    private void ensureOwnNotifications(Integer usuarioId) {
        if (sessionManager.getCurrentUser() == null
                || !sessionManager.getCurrentUser().getId().equals(usuarioId)) {
            throw new SecurityException("Solo puede consultar sus propias notificaciones.");
        }
    }
}
