package com.acme.sica.security;

import com.acme.sica.model.Permiso;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.PermisoRepository;
import com.acme.sica.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();
        // Agregar rol
        if (usuario.getRol() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombreRol().replace(" ", "_").toUpperCase()));
            // Cargar permisos granulares desde BD
            List<Permiso> permisos = permisoRepository.findPermisosByRolId(usuario.getRol().getId());
            for (Permiso p : permisos) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + p.getNombrePermiso()));
            }
        }

        return new CustomUserDetails(usuario, authorities);
    }
}
