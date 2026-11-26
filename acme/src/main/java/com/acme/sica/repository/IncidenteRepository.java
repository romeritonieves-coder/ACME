package com.acme.sica.repository;

import com.acme.sica.model.Incidente;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;
import com.acme.sica.model.Visita;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class IncidenteRepository extends JdbcRepositorySupport {
    private static final String SELECT = """
            SELECT i.*, u.nombre AS reportado_por_nombre
            FROM incidentes i JOIN usuarios u ON u.id = i.reportado_por_id
            """;

    public Optional<Incidente> findById(Integer id) {
        return queryOne(SELECT + "WHERE i.id = ?", this::map, id);
    }

    public List<Incidente> findAll() {
        return findAllByOrderByFechaDesc();
    }

    public List<Incidente> findAllByOrderByFechaDesc() {
        return query(SELECT + "ORDER BY i.fecha DESC", this::map);
    }

    public List<Incidente> findByEstado(String estado) {
        return query(SELECT + "WHERE i.estado = ? ORDER BY i.fecha DESC", this::map, estado);
    }

    public List<Incidente> findBySeveridad(String severidad) {
        return query(SELECT + "WHERE i.severidad = ? ORDER BY i.fecha DESC", this::map, severidad);
    }

    public Incidente save(Incidente incident) {
        Integer visitId = incident.getVisita() == null ? null : incident.getVisita().getId();
        Integer reporterId = incident.getReportadoPor() == null ? null : incident.getReportadoPor().getId();
        Integer pointId = incident.getPuntoAcceso() == null ? null : incident.getPuntoAcceso().getId();
        if (incident.getId() == null) {
            incident.setId(insert("""
                    INSERT INTO incidentes
                    (visita_id, reportado_por_id, punto_acceso_id, fecha, severidad, estado, descripcion, acciones_tomadas)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, visitId, reporterId, pointId, incident.getFecha(), incident.getSeveridad(),
                    incident.getEstado(), incident.getDescripcion(), incident.getAccionesTomadas()));
        } else {
            update("""
                    UPDATE incidentes SET visita_id = ?, reportado_por_id = ?, punto_acceso_id = ?, fecha = ?,
                    severidad = ?, estado = ?, descripcion = ?, acciones_tomadas = ? WHERE id = ?
                    """, visitId, reporterId, pointId, incident.getFecha(), incident.getSeveridad(),
                    incident.getEstado(), incident.getDescripcion(), incident.getAccionesTomadas(), incident.getId());
        }
        return incident;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM incidentes WHERE id = ?", id);
    }

    private Incidente map(ResultSet rs) throws SQLException {
        Timestamp date = rs.getTimestamp("fecha");
        Integer pointId = nullableId(rs, "punto_acceso_id");
        return Incidente.builder().id(rs.getInt("id"))
                .visita(Visita.builder().id(rs.getInt("visita_id")).build())
                .reportadoPor(Usuario.builder().id(rs.getInt("reportado_por_id"))
                        .nombre(rs.getString("reportado_por_nombre")).build())
                .puntoAcceso(pointId == null ? null : PuntoAcceso.builder().id(pointId).build())
                .fecha(date == null ? null : date.toLocalDateTime()).severidad(rs.getString("severidad"))
                .estado(rs.getString("estado")).descripcion(rs.getString("descripcion"))
                .accionesTomadas(rs.getString("acciones_tomadas")).build();
    }

    private static Integer nullableId(ResultSet rs, String column) throws SQLException {
        int id = rs.getInt(column);
        return rs.wasNull() ? null : id;
    }
}
