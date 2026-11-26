package com.acme.sica.repository;

import com.acme.sica.model.Notificacion;
import com.acme.sica.model.Usuario;
import com.acme.sica.model.Visita;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class NotificacionRepository extends JdbcRepositorySupport {
    public Optional<Notificacion> findById(Long id) {
        return queryOne("SELECT * FROM notificaciones WHERE id = ?", this::map, id);
    }

    public List<Notificacion> findByDestinatarioUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId) {
        return query("SELECT * FROM notificaciones WHERE destinatario_usuario_id = ? ORDER BY fecha_creacion DESC",
                this::map, usuarioId);
    }

    public List<Notificacion> findTop10ByDestinatarioUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId) {
        return query("""
                SELECT * FROM notificaciones
                WHERE destinatario_usuario_id = ?
                ORDER BY fecha_creacion DESC
                LIMIT 10
                """, this::map, usuarioId);
    }

    public List<Notificacion> findByDestinatarioUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(Integer usuarioId) {
        return query("""
                SELECT * FROM notificaciones WHERE destinatario_usuario_id = ? AND leida = FALSE
                ORDER BY fecha_creacion DESC
                """, this::map, usuarioId);
    }

    public List<Notificacion> findAll() {
        return query("SELECT * FROM notificaciones ORDER BY fecha_creacion DESC", this::map);
    }

    public Notificacion save(Notificacion notification) {
        Integer visitId = notification.getVisita() == null ? null : notification.getVisita().getId();
        Integer userId = notification.getDestinatarioUsuario() == null
                ? null : notification.getDestinatarioUsuario().getId();
        if (notification.getId() == null) {
            notification.setId((long) insert("""
                    INSERT INTO notificaciones
                    (visita_id, destinatario_usuario_id, tipo, mensaje, leida, fecha_lectura)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, visitId, userId, notification.getTipo(), notification.getMensaje(),
                    notification.getLeida(), notification.getFechaLectura()));
        } else {
            update("""
                    UPDATE notificaciones SET visita_id = ?, destinatario_usuario_id = ?, tipo = ?,
                    mensaje = ?, leida = ?, fecha_lectura = ? WHERE id = ?
                    """, visitId, userId, notification.getTipo(), notification.getMensaje(),
                    notification.getLeida(), notification.getFechaLectura(), notification.getId());
        }
        return notification;
    }

    public void deleteById(Long id) {
        update("DELETE FROM notificaciones WHERE id = ?", id);
    }

    private Notificacion map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("fecha_creacion");
        Timestamp read = rs.getTimestamp("fecha_lectura");
        return Notificacion.builder().id(rs.getLong("id"))
                .visita(Visita.builder().id(rs.getInt("visita_id")).build())
                .destinatarioUsuario(Usuario.builder().id(rs.getInt("destinatario_usuario_id")).build())
                .tipo(rs.getString("tipo")).mensaje(rs.getString("mensaje")).leida(rs.getBoolean("leida"))
                .fechaCreacion(created == null ? null : created.toLocalDateTime())
                .fechaLectura(read == null ? null : read.toLocalDateTime()).build();
    }
}
