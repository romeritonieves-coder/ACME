package com.acme.sica.pattern.observer;

import com.acme.sica.model.Notificacion;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.NotificacionRepository;
import com.acme.sica.repository.UsuarioRepository;
import java.util.logging.Logger;

/**
 * PATRÓN OBSERVER (Concrete Observer 2):
 * Persiste las notificaciones operacionales en la tabla `notificaciones` de MySQL
 * para su consulta persistente en la bandeja del Funcionario o Guarda.
 */
public class DatabaseNotificacionObserver implements AccesoObserver {
    private static final Logger LOGGER = Logger.getLogger(DatabaseNotificacionObserver.class.getName());

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public DatabaseNotificacionObserver(NotificacionRepository notificacionRepository,
                                        UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onEventoAcceso(AccesoEvento evento) {
        // Solo persistir eventos que requieran atención en bandeja
        if (evento.getDestinatarioUsuarioId() == null || evento.getVisita() == null) {
            return;
        }

        String tipo = evento.getTipo();
        if ("SOLICITUD_INGRESO".equals(tipo) || "PASE_TEMPORAL".equals(tipo) || "RESPUESTA_APROBACION".equals(tipo) || "ALERTA_SEGURIDAD".equals(tipo)) {
            Usuario destinatario = usuarioRepository.findById(evento.getDestinatarioUsuarioId())
                    .orElseThrow(() -> new IllegalStateException("Notification recipient was not found."));
            Notificacion notif = Notificacion.builder().visita(evento.getVisita())
                    .destinatarioUsuario(destinatario).tipo(tipo).mensaje(evento.getMensaje())
                    .leida(false).build();
            notificacionRepository.save(notif);
            LOGGER.fine(() -> "Persisted access notification " + notif.getId());
        }
    }
}
