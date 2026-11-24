package com.acme.sica.security;

import com.acme.sica.model.Usuario;
import com.acme.sica.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validador granular de permisos RBAC consultando activamente la base de datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {

    private final PermisoRepository permisoRepository;

    @Transactional(readOnly = true)
    public boolean tienePermiso(Usuario usuario, String nombrePermiso) {
        if (usuario == null || usuario.getRol() == null) {
            log.warn("RBAC Denegado: Usuario o rol nulo");
            return false;
        }

        // Superusuario tiene permiso absoluto
        if ("Superusuario".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            return true;
        }

        // Verificación granular directa contra la tabla rol_permisos en MySQL
        boolean autorizado = permisoRepository.rolTienePermiso(usuario.getRol().getId(), nombrePermiso);
        if (!autorizado) {
            log.warn("RBAC Denegado: Rol '{}' no posee permiso '{}'", usuario.getRol().getNombreRol(), nombrePermiso);
        }
        return autorizado;
    }

    public void validarPermiso(Usuario usuario, String nombrePermiso) {
        if (!tienePermiso(usuario, nombrePermiso)) {
            throw new SecurityException("Acceso Denegado por RBAC: Se requiere el permiso granular '" + nombrePermiso + "' en BD.");
        }
    }
}
