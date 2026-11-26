package com.acme.sica.repository;

import com.acme.sica.model.Empresa;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PersonaEstadoAcceso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class PersonaRepository extends JdbcRepositorySupport {
    private static final String SELECT = """
            SELECT p.id, p.nombre, p.documento_identidad, p.tipo_persona, p.motivo_restriccion,
                   p.telefono, p.url_foto, p.created_at, p.updated_at,
                   e.id AS empresa_id, e.nombre AS empresa_nombre, e.nit AS empresa_nit,
                   e.ubicacion AS empresa_ubicacion, e.contacto_principal AS empresa_contacto,
                   e.telefono AS empresa_telefono, e.esta_activa AS empresa_activa,
                   s.id AS estado_id, s.nombre_estado AS estado_nombre
            FROM personas p
            LEFT JOIN empresas e ON e.id = p.empresa_id
            JOIN persona_estados_acceso s ON s.id = p.estado_acceso_id
            """;

    public Optional<Persona> findById(Integer id) {
        return queryOne(SELECT + " WHERE p.id = ?", this::map, id);
    }

    public Optional<Persona> findByDocumentoIdentidad(String documentoIdentidad) {
        return queryOne(SELECT + " WHERE p.documento_identidad = ?", this::map, documentoIdentidad);
    }

    public List<Persona> findByEmpresaId(Integer empresaId) {
        return query(SELECT + " WHERE p.empresa_id = ? ORDER BY p.nombre", this::map, empresaId);
    }

    public List<Persona> findByTipoPersona(String tipoPersona) {
        return query(SELECT + " WHERE p.tipo_persona = ? ORDER BY p.nombre", this::map, tipoPersona);
    }

    public List<Persona> buscarPorNombreODocumento(String query) {
        String filter = "%" + query + "%";
        return query(SELECT + " WHERE LOWER(p.nombre) LIKE LOWER(?) OR p.documento_identidad LIKE ? ORDER BY p.nombre",
                this::map, filter, filter);
    }

    public List<Persona> findAll() {
        return query(SELECT + " ORDER BY p.nombre", this::map);
    }

    public Persona save(Persona persona) {
        Integer empresaId = persona.getEmpresa() == null ? null : persona.getEmpresa().getId();
        Integer estadoId = persona.getEstadoAcceso() == null ? null : persona.getEstadoAcceso().getId();
        if (persona.getId() == null) {
            persona.setId(insert("""
                    INSERT INTO personas
                    (nombre, documento_identidad, empresa_id, tipo_persona, estado_acceso_id,
                     motivo_restriccion, telefono, url_foto)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, persona.getNombre(), persona.getDocumentoIdentidad(), empresaId, persona.getTipoPersona(),
                    estadoId, persona.getMotivoRestriccion(), persona.getTelefono(), persona.getUrlFoto()));
        } else {
            update("""
                    UPDATE personas SET nombre = ?, documento_identidad = ?, empresa_id = ?, tipo_persona = ?,
                    estado_acceso_id = ?, motivo_restriccion = ?, telefono = ?, url_foto = ? WHERE id = ?
                    """, persona.getNombre(), persona.getDocumentoIdentidad(), empresaId, persona.getTipoPersona(),
                    estadoId, persona.getMotivoRestriccion(), persona.getTelefono(), persona.getUrlFoto(), persona.getId());
        }
        return persona;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM personas WHERE id = ?", id);
    }

    private Persona map(ResultSet rs) throws SQLException {
        Empresa empresa = null;
        int empresaId = rs.getInt("empresa_id");
        if (!rs.wasNull()) {
            empresa = Empresa.builder().id(empresaId).nombre(rs.getString("empresa_nombre"))
                    .nit(rs.getString("empresa_nit")).ubicacion(rs.getString("empresa_ubicacion"))
                    .contactoPrincipal(rs.getString("empresa_contacto")).telefono(rs.getString("empresa_telefono"))
                    .estaActiva(rs.getBoolean("empresa_activa")).build();
        }
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return Persona.builder().id(rs.getInt("id")).nombre(rs.getString("nombre"))
                .documentoIdentidad(rs.getString("documento_identidad")).empresa(empresa)
                .tipoPersona(rs.getString("tipo_persona"))
                .estadoAcceso(PersonaEstadoAcceso.builder().id(rs.getInt("estado_id"))
                        .nombreEstado(rs.getString("estado_nombre")).build())
                .motivoRestriccion(rs.getString("motivo_restriccion")).telefono(rs.getString("telefono"))
                .urlFoto(rs.getString("url_foto"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .updatedAt(updated == null ? null : updated.toLocalDateTime()).build();
    }
}
