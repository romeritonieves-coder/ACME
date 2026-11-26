package com.acme.sica.repository;

import com.acme.sica.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class VisitaRepository extends JdbcRepositorySupport {
    private static final String SELECT = "SELECT v.* FROM visitas v ";
    private final PersonaRepository personas = new PersonaRepository();
    private final VisitaEstadoRepository estados = new VisitaEstadoRepository();
    private final EmpresaRepository empresas = new EmpresaRepository();
    private final UsuarioRepository usuarios = new UsuarioRepository();
    private final PuntoAccesoRepository puntos = new PuntoAccesoRepository();
    private final VehiculoRepository vehiculos = new VehiculoRepository();

    public Optional<Visita> findById(Integer id) {
        return queryOne(SELECT + "WHERE v.id = ?", this::map, id);
    }

    public Optional<Visita> findByCodigoPase(String codigoPase) {
        return queryOne(SELECT + "WHERE v.codigo_pase = ?", this::map, codigoPase);
    }

    public List<Visita> findByPersonaIdOrderByCreatedAtDesc(Integer personaId) {
        return query(SELECT + "WHERE v.persona_id = ? ORDER BY v.created_at DESC", this::map, personaId);
    }

    public List<Visita> findVisitasActivasDentroPorPersona(Integer personaId) {
        return query(SELECT + """
                JOIN visita_estados s ON s.id = v.estado_visita_id
                WHERE v.persona_id = ? AND s.nombre_estado = 'Dentro'
                ORDER BY v.fecha_entrada DESC
                """, this::map, personaId);
    }

    public List<Visita> findVisitasAprobadasPorPersona(Integer personaId) {
        return query(SELECT + """
                JOIN visita_estados s ON s.id = v.estado_visita_id
                WHERE v.persona_id = ? AND s.nombre_estado = 'Aprobado'
                ORDER BY v.id DESC
                """, this::map, personaId);
    }

    public List<Visita> findSolicitudesPendientes() {
        return query(SELECT + """
                JOIN visita_estados s ON s.id = v.estado_visita_id
                WHERE s.nombre_estado IN ('Pendiente de Aprobacion', 'Pendiente de Aprobacion por Olvido')
                ORDER BY v.created_at DESC
                """, this::map);
    }

    public List<Visita> findSolicitudesPendientesPorAnfitrion(Integer anfitrionId) {
        return query(SELECT + """
                JOIN visita_estados s ON s.id = v.estado_visita_id
                WHERE v.anfitrion_id = ?
                  AND s.nombre_estado IN ('Pendiente de Aprobacion', 'Pendiente de Aprobacion por Olvido')
                ORDER BY v.created_at DESC
                """, this::map, anfitrionId);
    }

    public List<Visita> findAll() {
        return query(SELECT + "ORDER BY v.created_at DESC", this::map);
    }

    public Visita save(Visita visit) {
        Integer personaId = idOf(visit.getPersona());
        Integer companyId = idOf(visit.getEmpresaDestino());
        Integer hostId = idOf(visit.getAnfitrion());
        Integer registeredById = idOf(visit.getRegistradoPor());
        Integer approvedById = idOf(visit.getVisitaAprobadaPor());
        Integer entryPointId = idOf(visit.getPuntoAccesoEntrada());
        Integer exitPointId = idOf(visit.getPuntoAccesoSalida());
        Integer vehicleId = idOf(visit.getVehiculo());
        Integer stateId = idOf(visit.getEstadoVisita());
        if (visit.getId() == null) {
            visit.setId(insert("""
                    INSERT INTO visitas (persona_id, empresa_destino_id, anfitrion_id, registrado_por_id,
                    visita_aprobada_por, punto_acceso_entrada_id, punto_acceso_salida_id, vehiculo_id,
                    vehiculo_placa, codigo_pase, es_pase_temporal, fecha_programada, fecha_entrada, fecha_salida,
                    estado_visita_id, motivo_visita, observaciones)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, personaId, companyId, hostId, registeredById, approvedById, entryPointId, exitPointId,
                    vehicleId, visit.getVehiculoPlaca(), visit.getCodigoPase(), visit.getEsPaseTemporal(),
                    visit.getFechaProgramada(), visit.getFechaEntrada(), visit.getFechaSalida(), stateId,
                    visit.getMotivoVisita(), visit.getObservaciones()));
        } else {
            update("""
                    UPDATE visitas SET persona_id = ?, empresa_destino_id = ?, anfitrion_id = ?, registrado_por_id = ?,
                    visita_aprobada_por = ?, punto_acceso_entrada_id = ?, punto_acceso_salida_id = ?, vehiculo_id = ?,
                    vehiculo_placa = ?, codigo_pase = ?, es_pase_temporal = ?, fecha_programada = ?, fecha_entrada = ?,
                    fecha_salida = ?, estado_visita_id = ?, motivo_visita = ?, observaciones = ? WHERE id = ?
                    """, personaId, companyId, hostId, registeredById, approvedById, entryPointId, exitPointId,
                    vehicleId, visit.getVehiculoPlaca(), visit.getCodigoPase(), visit.getEsPaseTemporal(),
                    visit.getFechaProgramada(), visit.getFechaEntrada(), visit.getFechaSalida(), stateId,
                    visit.getMotivoVisita(), visit.getObservaciones(), visit.getId());
        }
        return visit;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM visitas WHERE id = ?", id);
    }

    private Visita map(ResultSet rs) throws SQLException {
        Integer personaId = nullableInt(rs, "persona_id");
        Integer companyId = nullableInt(rs, "empresa_destino_id");
        Integer hostId = nullableInt(rs, "anfitrion_id");
        Integer registeredId = nullableInt(rs, "registrado_por_id");
        Integer approvedId = nullableInt(rs, "visita_aprobada_por");
        Integer entryPointId = nullableInt(rs, "punto_acceso_entrada_id");
        Integer exitPointId = nullableInt(rs, "punto_acceso_salida_id");
        Integer vehicleId = nullableInt(rs, "vehiculo_id");
        Integer stateId = nullableInt(rs, "estado_visita_id");
        Timestamp scheduled = rs.getTimestamp("fecha_programada");
        Timestamp entered = rs.getTimestamp("fecha_entrada");
        Timestamp exited = rs.getTimestamp("fecha_salida");
        Timestamp created = rs.getTimestamp("created_at");
        return Visita.builder().id(rs.getInt("id"))
                .persona(personaId == null ? null : personas.findById(personaId).orElse(null))
                .empresaDestino(companyId == null ? null : empresas.findById(companyId).orElse(null))
                .anfitrion(hostId == null ? null : usuarios.findById(hostId).orElse(null))
                .registradoPor(registeredId == null ? null : usuarios.findById(registeredId).orElse(null))
                .visitaAprobadaPor(approvedId == null ? null : usuarios.findById(approvedId).orElse(null))
                .puntoAccesoEntrada(entryPointId == null ? null : puntos.findById(entryPointId).orElse(null))
                .puntoAccesoSalida(exitPointId == null ? null : puntos.findById(exitPointId).orElse(null))
                .vehiculo(vehicleId == null ? null : vehiculos.findById(vehicleId).orElse(null))
                .vehiculoPlaca(rs.getString("vehiculo_placa")).codigoPase(rs.getString("codigo_pase"))
                .esPaseTemporal(rs.getBoolean("es_pase_temporal"))
                .fechaProgramada(scheduled == null ? null : scheduled.toLocalDateTime())
                .fechaEntrada(entered == null ? null : entered.toLocalDateTime())
                .fechaSalida(exited == null ? null : exited.toLocalDateTime())
                .estadoVisita(stateId == null ? null : estados.findById(stateId).orElse(null))
                .motivoVisita(rs.getString("motivo_visita")).observaciones(rs.getString("observaciones"))
                .createdAt(created == null ? null : created.toLocalDateTime()).build();
    }

    private static Integer idOf(Object entity) {
        if (entity instanceof Persona value) return value.getId();
        if (entity instanceof Empresa value) return value.getId();
        if (entity instanceof Usuario value) return value.getId();
        if (entity instanceof PuntoAcceso value) return value.getId();
        if (entity instanceof Vehiculo value) return value.getId();
        if (entity instanceof VisitaEstado value) return value.getId();
        return null;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
