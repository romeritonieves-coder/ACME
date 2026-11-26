package com.acme.sica.repository;

import com.acme.sica.model.VwSolicitudesPendientes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class VwSolicitudesPendientesRepository extends JdbcRepositorySupport {
    public List<VwSolicitudesPendientes> findAll() {
        return query("""
                SELECT visita_id, visitante, documento_identidad, tipo_persona, estado,
                       empresa_destino, anfitrion, es_pase_temporal, fecha_solicitud
                FROM vw_solicitudes_pendientes ORDER BY fecha_solicitud DESC
                """, this::map);
    }

    private VwSolicitudesPendientes map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("fecha_solicitud");
        return new VwSolicitudesPendientes(rs.getInt("visita_id"), rs.getString("visitante"),
                rs.getString("documento_identidad"), rs.getString("tipo_persona"), rs.getString("estado"),
                rs.getString("empresa_destino"), rs.getString("anfitrion"), rs.getBoolean("es_pase_temporal"),
                created == null ? null : created.toLocalDateTime());
    }
}
