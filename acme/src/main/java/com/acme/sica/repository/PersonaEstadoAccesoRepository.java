package com.acme.sica.repository;

import com.acme.sica.model.PersonaEstadoAcceso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PersonaEstadoAccesoRepository extends JdbcRepositorySupport {
    public Optional<PersonaEstadoAcceso> findById(Integer id) {
        return queryOne("SELECT id, nombre_estado FROM persona_estados_acceso WHERE id = ?",
                this::map, id);
    }

    public Optional<PersonaEstadoAcceso> findByNombreEstado(String nombreEstado) {
        return queryOne("SELECT id, nombre_estado FROM persona_estados_acceso WHERE nombre_estado = ?",
                this::map, nombreEstado);
    }

    public List<PersonaEstadoAcceso> findAll() {
        return query("SELECT id, nombre_estado FROM persona_estados_acceso ORDER BY id", this::map);
    }

    public PersonaEstadoAcceso save(PersonaEstadoAcceso estado) {
        if (estado.getId() == null) {
            estado.setId(insert("INSERT INTO persona_estados_acceso (nombre_estado) VALUES (?)",
                    estado.getNombreEstado()));
        } else {
            update("UPDATE persona_estados_acceso SET nombre_estado = ? WHERE id = ?",
                    estado.getNombreEstado(), estado.getId());
        }
        return estado;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM persona_estados_acceso WHERE id = ?", id);
    }

    private PersonaEstadoAcceso map(ResultSet rs) throws SQLException {
        return PersonaEstadoAcceso.builder()
                .id(rs.getInt("id"))
                .nombreEstado(rs.getString("nombre_estado"))
                .build();
    }
}
