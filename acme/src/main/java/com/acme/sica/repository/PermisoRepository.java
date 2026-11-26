package com.acme.sica.repository;

import com.acme.sica.model.Permiso;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PermisoRepository extends JdbcRepositorySupport {
    public Optional<Permiso> findById(Integer id) {
        return queryOne("SELECT id, modulo, nombre_permiso, descripcion FROM permisos WHERE id = ?",
                this::map, id);
    }

    public Optional<Permiso> findByNombrePermiso(String nombrePermiso) {
        return queryOne("SELECT id, modulo, nombre_permiso, descripcion FROM permisos WHERE nombre_permiso = ?",
                this::map, nombrePermiso);
    }

    public List<Permiso> findPermisosByRolId(Integer rolId) {
        return query("""
                SELECT p.id, p.modulo, p.nombre_permiso, p.descripcion
                FROM permisos p JOIN rol_permisos rp ON rp.permiso_id = p.id
                WHERE rp.rol_id = ? ORDER BY p.modulo, p.nombre_permiso
                """, this::map, rolId);
    }

    public boolean rolTienePermiso(Integer rolId, String nombrePermiso) {
        return queryOne("""
                SELECT 1 FROM rol_permisos rp JOIN permisos p ON p.id = rp.permiso_id
                WHERE rp.rol_id = ? AND p.nombre_permiso = ? LIMIT 1
                """, rs -> rs.getInt(1), rolId, nombrePermiso).isPresent();
    }

    public List<Permiso> findAll() {
        return query("SELECT id, modulo, nombre_permiso, descripcion FROM permisos ORDER BY id", this::map);
    }

    public Permiso save(Permiso permiso) {
        if (permiso.getId() == null) {
            permiso.setId(insert("INSERT INTO permisos (modulo, nombre_permiso, descripcion) VALUES (?, ?, ?)",
                    permiso.getModulo(), permiso.getNombrePermiso(), permiso.getDescripcion()));
        } else {
            update("UPDATE permisos SET modulo = ?, nombre_permiso = ?, descripcion = ? WHERE id = ?",
                    permiso.getModulo(), permiso.getNombrePermiso(), permiso.getDescripcion(), permiso.getId());
        }
        return permiso;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM permisos WHERE id = ?", id);
    }

    private Permiso map(ResultSet rs) throws SQLException {
        return Permiso.builder().id(rs.getInt("id")).modulo(rs.getString("modulo"))
                .nombrePermiso(rs.getString("nombre_permiso")).descripcion(rs.getString("descripcion")).build();
    }
}
