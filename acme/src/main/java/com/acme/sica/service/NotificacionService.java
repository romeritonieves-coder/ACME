package com.acme.sica.service;

import com.acme.sica.model.Notificacion;
import com.acme.sica.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional(readOnly = true)
    public List<Notificacion> obtenerPorUsuario(Integer usuarioId) {
        return notificacionRepository.findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> obtenerNoLeidas(Integer usuarioId) {
        return notificacionRepository.findByDestinatarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);
    }

    @Transactional
    public void marcarComoLeida(Long notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(n -> {
            n.setLeida(true);
            n.setFechaLectura(LocalDateTime.now());
            notificacionRepository.save(n);
        });
    }
}
