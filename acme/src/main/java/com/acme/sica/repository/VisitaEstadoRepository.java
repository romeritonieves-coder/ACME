package com.acme.sica.repository;

import com.acme.sica.model.VisitaEstado;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class VisitaEstadoRepository extends JdbcRepositorySupport {
    public Optional<VisitaEstado> findById(Integer id) {
        return queryOne("SELECT id, nombre_estado FROM visita_estados WHERE id = ?", this::map, id);
    }

    public Optional<VisitaEstado> findByNombreEstado(String nombreEstado) {
        return queryOne("SELECT id, nombre_estado FROM visita_estados WHERE nombre_estado = ?",
                this::map, nombreEstado);
    }

    public List<VisitaEstado> findAll() {
        return query("SELECT id, nombre_estado FROM visita_estados ORDER BY id", this::map);
    }

    public VisitaEstado save(VisitaEstado estado) {
        if (estado.getId() == null) {
            estado.setId(insert("INSERT INTO visita_estados (nombre_estado) VALUES (?)", estado.getNombreEstado()));
        } else {
            update("UPDATE visita_estados SET nombre_estado = ? WHERE id = ?",
                    estado.getNombreEstado(), estado.getId());
        }
        return estado;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM visita_estados WHERE id = ?", id);
    }

    private VisitaEstado map(ResultSet rs) throws SQLException {
        return VisitaEstado.builder().id(rs.getInt("id")).nombreEstado(rs.getString("nombre_estado")).build();
    }
}
