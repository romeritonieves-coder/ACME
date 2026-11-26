package com.acme.sica.security;

import com.acme.sica.model.Permiso;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.PermisoRepository;

import java.util.Set;
import java.util.stream.Collectors;

public class RbacService {
    private final PermisoRepository permisoRepository;
    private final SessionManager sessionManager;

    public RbacService() {
        this(new PermisoRepository(), SessionManager.getInstance());
    }

    public RbacService(PermisoRepository permisoRepository, SessionManager sessionManager) {
        this.permisoRepository = permisoRepository;
        this.sessionManager = sessionManager;
    }

    public Set<String> permisosParaRol(Integer rolId) {
        return permisoRepository.findPermisosByRolId(rolId).stream()
                .map(Permiso::getNombrePermiso)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean tienePermiso(Usuario usuario, String nombrePermiso) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        if ("Superusuario".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            return true;
        }
        return permisoRepository.rolTienePermiso(usuario.getRol().getId(), nombrePermiso);
    }

    public void validarPermiso(Usuario usuario, String nombrePermiso) {
        if (usuario == null || !tienePermiso(usuario, nombrePermiso)) {
            throw new SecurityException("Acceso denegado: se requiere el permiso " + nombrePermiso + ".");
        }
    }

    public void validarSesion(String nombrePermiso) {
        sessionManager.requirePermission(nombrePermiso);
    }
}
