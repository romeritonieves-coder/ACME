package com.acme.sica.pattern.observer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER (Concrete Observer 1):
 * Publica eventos a través de WebSocket (STOMP) a los canales de portería,
 * anfitriones específicos y evacuación en tiempo real.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAccesoObserver implements AccesoObserver {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onEventoAcceso(AccesoEvento evento) {
        try {
            // Canal general de portería (para guardas y supervisores)
            messagingTemplate.convertAndSend("/topic/porteria", evento);

            // Canal específico para el funcionario anfitrión si aplica
            if (evento.getDestinatarioUsuarioId() != null) {
                messagingTemplate.convertAndSend("/topic/funcionario/" + evento.getDestinatarioUsuarioId(), evento);
            }

            // Notificación al canal de evacuación si hubo check-in o check-out
            if ("CHECK_IN".equals(evento.getTipo()) || "CHECK_OUT".equals(evento.getTipo()) || "REGULARIZACION".equals(evento.getTipo())) {
                messagingTemplate.convertAndSend("/topic/evacuacion", evento);
            }

            log.info("WebSocket emitido evento '{}' a /topic/porteria y destinatario {}", evento.getTipo(), evento.getDestinatarioUsuarioId());
        } catch (Exception e) {
            log.error("Error al enviar evento por WebSocket: {}", e.getMessage());
        }
    }
}
