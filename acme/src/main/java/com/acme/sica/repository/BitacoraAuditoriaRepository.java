package com.acme.sica.repository;

import com.acme.sica.model.BitacoraAuditoria;
import com.acme.sica.model.Usuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class BitacoraAuditoriaRepository extends JdbcRepositorySupport {
    private static final String SELECT = """
            SELECT a.*, u.nombre AS auditoria_usuario_nombre, u.email AS auditoria_usuario_email
            FROM bitacora_auditoria a LEFT JOIN usuarios u ON u.id = a.usuario_id
            """;

    public Optional<BitacoraAuditoria> findById(Long id) {
        return queryOne(SELECT + "WHERE a.id = ?", this::map, id);
    }

    public BitacoraAuditoria save(BitacoraAuditoria audit) {
        Integer userId = audit.getUsuario() == null ? null : audit.getUsuario().getId();
        int id = insert("""
                INSERT INTO bitacora_auditoria
                (usuario_id, fecha_hora, accion_realizada, tabla_afectada, registro_id_afectado, ip_origen, detalles)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, userId, audit.getFechaHora(), audit.getAccionRealizada(), audit.getTablaAfectada(),
                audit.getRegistroIdAfectado(), audit.getIpOrigen(), audit.getDetalles());
        audit.setId((long) id);
        return audit;
    }

    public List<BitacoraAuditoria> findAllByOrderByFechaHoraDesc() {
        return query(SELECT + "ORDER BY a.fecha_hora DESC", this::map);
    }

    public List<BitacoraAuditoria> findTop100ByOrderByFechaHoraDesc() {
        return query(SELECT + "ORDER BY a.fecha_hora DESC LIMIT 100", this::map);
    }

    public List<BitacoraAuditoria> findByUsuarioIdOrderByFechaHoraDesc(Integer usuarioId) {
        return query(SELECT + "WHERE a.usuario_id = ? ORDER BY a.fecha_hora DESC",
                this::map, usuarioId);
    }

    private BitacoraAuditoria map(ResultSet rs) throws SQLException {
        int userId = rs.getInt("usuario_id");
        Usuario user = rs.wasNull() ? null : Usuario.builder().id(userId)
                .nombre(rs.getString("auditoria_usuario_nombre"))
                .email(rs.getString("auditoria_usuario_email")).build();
        Timestamp at = rs.getTimestamp("fecha_hora");
        return BitacoraAuditoria.builder().id(rs.getLong("id")).usuario(user)
                .fechaHora(at == null ? null : at.toLocalDateTime())
                .accionRealizada(rs.getString("accion_realizada")).tablaAfectada(rs.getString("tabla_afectada"))
                .registroIdAfectado((Integer) rs.getObject("registro_id_afectado"))
                .ipOrigen(rs.getString("ip_origen")).detalles(rs.getString("detalles")).build();
    }
}
