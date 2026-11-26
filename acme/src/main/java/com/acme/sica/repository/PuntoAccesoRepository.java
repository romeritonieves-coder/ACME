package com.acme.sica.repository;

import com.acme.sica.model.PuntoAcceso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PuntoAccesoRepository extends JdbcRepositorySupport {
    public Optional<PuntoAcceso> findById(Integer id) {
        return queryOne("SELECT id, nombre, tipo, esta_activo FROM puntos_acceso WHERE id = ?", this::map, id);
    }

    public Optional<PuntoAcceso> findByNombre(String nombre) {
        return queryOne("SELECT id, nombre, tipo, esta_activo FROM puntos_acceso WHERE nombre = ?", this::map, nombre);
    }

    public List<PuntoAcceso> findAll() {
        return query("SELECT id, nombre, tipo, esta_activo FROM puntos_acceso ORDER BY nombre", this::map);
    }

    public List<PuntoAcceso> findByEstaActivoTrue() {
        return query("SELECT id, nombre, tipo, esta_activo FROM puntos_acceso WHERE esta_activo = TRUE ORDER BY nombre",
                this::map);
    }

    public PuntoAcceso save(PuntoAcceso punto) {
        if (punto.getId() == null) {
            punto.setId(insert("INSERT INTO puntos_acceso (nombre, tipo, esta_activo) VALUES (?, ?, ?)",
                    punto.getNombre(), punto.getTipo(), punto.getEstaActivo()));
        } else {
            update("UPDATE puntos_acceso SET nombre = ?, tipo = ?, esta_activo = ? WHERE id = ?",
                    punto.getNombre(), punto.getTipo(), punto.getEstaActivo(), punto.getId());
        }
        return punto;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM puntos_acceso WHERE id = ?", id);
    }

    private PuntoAcceso map(ResultSet rs) throws SQLException {
        return PuntoAcceso.builder().id(rs.getInt("id")).nombre(rs.getString("nombre"))
                .tipo(rs.getString("tipo")).estaActivo(rs.getBoolean("esta_activo")).build();
    }
}
