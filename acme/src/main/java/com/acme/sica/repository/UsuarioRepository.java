package com.acme.sica.repository;

import com.acme.sica.model.Empresa;
import com.acme.sica.model.Rol;
import com.acme.sica.model.Usuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository extends JdbcRepositorySupport {
    private static final String SELECT = """
            SELECT u.id, u.nombre, u.email, u.password, u.esta_activo, u.intentos_fallidos,
                   u.ultimo_acceso, u.created_at, u.updated_at,
                   r.id AS rol_id, r.nombre_rol, r.descripcion AS rol_descripcion,
                   e.id AS empresa_id, e.nombre AS empresa_nombre, e.nit AS empresa_nit,
                   e.ubicacion AS empresa_ubicacion, e.contacto_principal AS empresa_contacto,
                   e.telefono AS empresa_telefono, e.esta_activa AS empresa_activa
            FROM usuarios u
            JOIN roles r ON r.id = u.rol_id
            LEFT JOIN empresas e ON e.id = u.empresa_id
            """;

    public Optional<Usuario> findById(Integer id) {
        return queryOne(SELECT + " WHERE u.id = ?", this::map, id);
    }

    public Optional<Usuario> findByEmail(String email) {
        return queryOne(SELECT + " WHERE u.email = ?", this::map, email);
    }

    public List<Usuario> findByRolId(Integer rolId) {
        return query(SELECT + " WHERE u.rol_id = ? ORDER BY u.nombre", this::map, rolId);
    }

    public List<Usuario> findByEmpresaId(Integer empresaId) {
        return query(SELECT + " WHERE u.empresa_id = ? ORDER BY u.nombre", this::map, empresaId);
    }

    public List<Usuario> findByEstaActivoTrue() {
        return query(SELECT + " WHERE u.esta_activo = TRUE ORDER BY u.nombre", this::map);
    }

    public List<Usuario> findAll() {
        return query(SELECT + " ORDER BY u.nombre", this::map);
    }

    public Usuario save(Usuario user) {
        Integer roleId = user.getRol() == null ? null : user.getRol().getId();
        Integer companyId = user.getEmpresa() == null ? null : user.getEmpresa().getId();
        if (user.getId() == null) {
            user.setId(insert("""
                    INSERT INTO usuarios (nombre, email, password, rol_id, empresa_id, esta_activo, intentos_fallidos, ultimo_acceso)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, user.getNombre(), user.getEmail(), user.getPassword(), roleId, companyId,
                    user.getEstaActivo(), user.getIntentosFallidos(), user.getUltimoAcceso()));
        } else {
            update("""
                    UPDATE usuarios SET nombre = ?, email = ?, password = ?, rol_id = ?, empresa_id = ?,
                    esta_activo = ?, intentos_fallidos = ?, ultimo_acceso = ? WHERE id = ?
                    """, user.getNombre(), user.getEmail(), user.getPassword(), roleId, companyId,
                    user.getEstaActivo(), user.getIntentosFallidos(), user.getUltimoAcceso(), user.getId());
        }
        return user;
    }

    public void deleteById(Integer id) {
        update("DELETE FROM usuarios WHERE id = ?", id);
    }

    private Usuario map(ResultSet rs) throws SQLException {
        Rol role = Rol.builder().id(rs.getInt("rol_id")).nombreRol(rs.getString("nombre_rol"))
                .descripcion(rs.getString("rol_descripcion")).build();
        Empresa company = null;
        int companyId = rs.getInt("empresa_id");
        if (!rs.wasNull()) {
            company = Empresa.builder().id(companyId).nombre(rs.getString("empresa_nombre"))
                    .nit(rs.getString("empresa_nit")).ubicacion(rs.getString("empresa_ubicacion"))
                    .contactoPrincipal(rs.getString("empresa_contacto")).telefono(rs.getString("empresa_telefono"))
                    .estaActiva(rs.getBoolean("empresa_activa")).build();
        }
        Timestamp lastLogin = rs.getTimestamp("ultimo_acceso");
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return Usuario.builder().id(rs.getInt("id")).nombre(rs.getString("nombre")).email(rs.getString("email"))
                .password(rs.getString("password")).rol(role).empresa(company).estaActivo(rs.getBoolean("esta_activo"))
                .intentosFallidos(rs.getInt("intentos_fallidos"))
                .ultimoAcceso(lastLogin == null ? null : lastLogin.toLocalDateTime())
                .createdAt(created == null ? null : created.toLocalDateTime())
                .updatedAt(updated == null ? null : updated.toLocalDateTime()).build();
    }
}
