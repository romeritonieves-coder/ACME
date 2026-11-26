package com.acme.sica.repository;

import com.acme.sica.model.Persona;
import com.acme.sica.model.Vehiculo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class VehiculoRepository extends JdbcRepositorySupport {
    public Optional<Vehiculo> findById(Integer id) {
        return queryOne("SELECT id, placa, tipo, marca_modelo, color, persona_id FROM vehiculos WHERE id = ?",
                this::map, id);
    }

    public Optional<Vehiculo> findByPlaca(String placa) {
        return queryOne("SELECT id, placa, tipo, marca_modelo, color, persona_id FROM vehiculos WHERE placa = ?",
                this::map, placa);
    }

    public List<Vehiculo> findAll() {
        return query("SELECT id, placa, tipo, marca_modelo, color, persona_id FROM vehiculos ORDER BY placa", this::map);
    }

    public Vehiculo save(Vehiculo vehiculo) {
        Integer personaId = vehiculo.getPersona() == null ? null : vehiculo.getPersona().getId();
        if (vehiculo.getId() == null) {
            vehiculo.setId(insert("""
                    INSERT INTO vehiculos (placa, tipo, marca_modelo, color, persona_id) VALUES (?, ?, ?, ?, ?)
                    """, vehiculo.getPlaca(), vehiculo.getTipo(), vehiculo.getMarcaModelo(),
                    vehiculo.getColor(), personaId));
        } else {
            update("""
                    UPDATE vehiculos SET placa = ?, tipo = ?, marca_modelo = ?, color = ?, persona_id = ?
                    WHERE id = ?
                    """, vehiculo.getPlaca(), vehiculo.getTipo(), vehiculo.getMarcaModelo(),
                    vehiculo.getColor(), personaId, vehiculo.getId());
        }
        return vehiculo;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM vehiculos WHERE id = ?", id);
    }

    private Vehiculo map(ResultSet rs) throws SQLException {
        int personaId = rs.getInt("persona_id");
        Persona persona = rs.wasNull() ? null : Persona.builder().id(personaId).build();
        return Vehiculo.builder().id(rs.getInt("id")).placa(rs.getString("placa")).tipo(rs.getString("tipo"))
                .marcaModelo(rs.getString("marca_modelo")).color(rs.getString("color")).persona(persona).build();
    }
}
