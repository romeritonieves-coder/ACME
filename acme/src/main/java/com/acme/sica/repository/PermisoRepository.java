package com.acme.sica.repository;

import com.acme.sica.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    Optional<Permiso> findByNombrePermiso(String nombrePermiso);

    @Query(value = "SELECT p.* FROM permisos p JOIN rol_permisos rp ON p.id = rp.permiso_id WHERE rp.rol_id = :rolId", nativeQuery = true)
    List<Permiso> findPermisosByRolId(@Param("rolId") Integer rolId);

    @Query(value = "SELECT COUNT(*) > 0 FROM rol_permisos rp JOIN permisos p ON rp.permiso_id = p.id WHERE rp.rol_id = :rolId AND p.nombre_permiso = :nombrePermiso", nativeQuery = true)
    boolean rolTienePermiso(@Param("rolId") Integer rolId, @Param("nombrePermiso") String nombrePermiso);
}
