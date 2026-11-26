package com.acme.sica.repository;

import com.acme.sica.model.Empresa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class EmpresaRepository extends JdbcRepositorySupport {
    private static final String COLUMNS = "id, nombre, nit, ubicacion, contacto_principal, telefono, esta_activa, created_at";

    public Optional<Empresa> findById(Integer id) {
        return queryOne("SELECT " + COLUMNS + " FROM empresas WHERE id = ?", this::map, id);
    }

    public Optional<Empresa> findByNombre(String nombre) {
        return queryOne("SELECT " + COLUMNS + " FROM empresas WHERE nombre = ?", this::map, nombre);
    }

    public Optional<Empresa> findByNit(String nit) {
        return queryOne("SELECT " + COLUMNS + " FROM empresas WHERE nit = ?", this::map, nit);
    }

    public List<Empresa> findAll() {
        return query("SELECT " + COLUMNS + " FROM empresas ORDER BY nombre", this::map);
    }

    public List<Empresa> findByEstaActivaTrue() {
        return query("SELECT " + COLUMNS + " FROM empresas WHERE esta_activa = TRUE ORDER BY nombre", this::map);
    }

    public Empresa save(Empresa empresa) {
        if (empresa.getId() == null) {
            empresa.setId(insert("""
                    INSERT INTO empresas (nombre, nit, ubicacion, contacto_principal, telefono, esta_activa)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, empresa.getNombre(), empresa.getNit(), empresa.getUbicacion(),
                    empresa.getContactoPrincipal(), empresa.getTelefono(), empresa.getEstaActiva()));
        } else {
            update("""
                    UPDATE empresas SET nombre = ?, nit = ?, ubicacion = ?, contacto_principal = ?,
                    telefono = ?, esta_activa = ? WHERE id = ?
                    """, empresa.getNombre(), empresa.getNit(), empresa.getUbicacion(),
                    empresa.getContactoPrincipal(), empresa.getTelefono(), empresa.getEstaActiva(), empresa.getId());
        }
        return empresa;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM empresas WHERE id = ?", id);
    }

    private Empresa map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return Empresa.builder().id(rs.getInt("id")).nombre(rs.getString("nombre")).nit(rs.getString("nit"))
                .ubicacion(rs.getString("ubicacion")).contactoPrincipal(rs.getString("contacto_principal"))
                .telefono(rs.getString("telefono")).estaActiva(rs.getBoolean("esta_activa"))
                .createdAt(created == null ? null : created.toLocalDateTime()).build();
    }
}
