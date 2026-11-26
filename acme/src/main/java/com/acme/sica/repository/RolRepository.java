package com.acme.sica.repository;

import com.acme.sica.model.Rol;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RolRepository extends JdbcRepositorySupport {
    public Optional<Rol> findById(Integer id) {
        return queryOne("SELECT id, nombre_rol, descripcion FROM roles WHERE id = ?", this::map, id);
    }

    public Optional<Rol> findByNombreRol(String nombreRol) {
        return queryOne("SELECT id, nombre_rol, descripcion FROM roles WHERE nombre_rol = ?", this::map, nombreRol);
    }

    public List<Rol> findAll() {
        return query("SELECT id, nombre_rol, descripcion FROM roles ORDER BY id", this::map);
    }

    public Rol save(Rol role) {
        if (role.getId() == null) {
            role.setId(insert("INSERT INTO roles (nombre_rol, descripcion) VALUES (?, ?)",
                    role.getNombreRol(), role.getDescripcion()));
        } else {
            update("UPDATE roles SET nombre_rol = ?, descripcion = ? WHERE id = ?",
                    role.getNombreRol(), role.getDescripcion(), role.getId());
        }
        return role;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM roles WHERE id = ?", id);
    }

    private Rol map(ResultSet rs) throws SQLException {
        return Rol.builder().id(rs.getInt("id")).nombreRol(rs.getString("nombre_rol"))
                .descripcion(rs.getString("descripcion")).build();
    }
}
