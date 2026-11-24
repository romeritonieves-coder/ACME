package com.acme.sica.repository;

import com.acme.sica.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    List<Notificacion> findByDestinatarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(Integer usuarioId);
}
