package com.acme.sica.pattern.observer;

import com.acme.sica.model.Notificacion;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.NotificacionRepository;
import com.acme.sica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATRÓN OBSERVER (Concrete Observer 2):
 * Persiste las notificaciones operacionales en la tabla `notificaciones` de MySQL
 * para su consulta persistente en la bandeja del Funcionario o Guarda.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseNotificacionObserver implements AccesoObserver {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void onEventoAcceso(AccesoEvento evento) {
        // Solo persistir eventos que requieran atención en bandeja
        if (evento.getDestinatarioUsuarioId() == null || evento.getVisita() == null) {
            return;
        }

        String tipo = evento.getTipo();
        if ("SOLICITUD_INGRESO".equals(tipo) || "PASE_TEMPORAL".equals(tipo) || "RESPUESTA_APROBACION".equals(tipo) || "ALERTA_SEGURIDAD".equals(tipo)) {
            Usuario destinatario = usuarioRepository.findById(evento.getDestinatarioUsuarioId()).orElse(null);
            if (destinatario != null) {
                Notificacion notif = Notificacion.builder()
                        .visita(evento.getVisita())
                        .destinatarioUsuario(destinatario)
                        .tipo(tipo)
                        .mensaje(evento.getMensaje())
                        .leida(false)
                        .build();
                notificacionRepository.save(notif);
                log.info("Notificación persistida en BD con ID {} para usuario {}", notif.getId(), destinatario.getEmail());
            }
        }
    }
}
