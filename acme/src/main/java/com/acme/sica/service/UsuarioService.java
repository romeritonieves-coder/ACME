package com.acme.sica.service;

import com.acme.sica.model.Empresa;
import com.acme.sica.model.Rol;
import com.acme.sica.model.Usuario;
import com.acme.sica.repository.EmpresaRepository;
import com.acme.sica.repository.RolRepository;
import com.acme.sica.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditedOperationProxy operations;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          EmpresaRepository empresaRepository, AuditedOperationProxy operations) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.operations = operations;
    }

    public List<Usuario> listar() {
        return operations.execute("USUARIOS_VER", "CONSULTAR_USUARIOS", "usuarios", null,
                usuarioRepository::findAll);
    }

    public Usuario crear(Usuario usuario, String passwordPlano) {
        return operations.execute("USUARIOS_CREAR", "CREAR_USUARIO", "usuarios", null, () -> {
            if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank()
                    || usuario.getEmail() == null || usuario.getEmail().isBlank()
                    || passwordPlano == null || passwordPlano.isBlank() || usuario.getRol() == null
                    || usuario.getRol().getId() == null) {
                throw new IllegalArgumentException("Nombre, correo, rol y contraseña son obligatorios.");
            }
            if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
            }
            Rol role = rolRepository.findById(usuario.getRol().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado."));
            Empresa company = loadCompany(usuario.getEmpresa());
            if ("Funcionario de Empresa".equalsIgnoreCase(role.getNombreRol()) && company == null) {
                throw new IllegalArgumentException("El rol Funcionario de Empresa requiere una empresa.");
            }
            usuario.setRol(role);
            usuario.setEmpresa(company);
            usuario.setPassword(BCrypt.hashpw(passwordPlano, BCrypt.gensalt(12)));
            usuario.setEstaActivo(true);
            usuario.setIntentosFallidos(0);
            return usuarioRepository.save(usuario);
        });
    }

    public Usuario actualizar(Usuario cambios, String nuevaPassword) {
        return operations.execute("USUARIOS_EDITAR", "ACTUALIZAR_USUARIO", "usuarios",
                cambios == null ? null : cambios.getId(), () -> {
            if (cambios == null || cambios.getId() == null) {
                throw new IllegalArgumentException("El usuario a actualizar debe tener un ID.");
            }
            Usuario user = usuarioRepository.findById(cambios.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) {
                user.setNombre(cambios.getNombre());
            }
            if (cambios.getEmail() != null && !cambios.getEmail().isBlank()) {
                usuarioRepository.findByEmail(cambios.getEmail()).filter(other -> !other.getId().equals(user.getId()))
                        .ifPresent(other -> { throw new IllegalArgumentException("El correo ya está en uso."); });
                user.setEmail(cambios.getEmail());
            }
            if (cambios.getRol() != null && cambios.getRol().getId() != null) {
                user.setRol(rolRepository.findById(cambios.getRol().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado.")));
            }
            if (cambios.getEmpresa() != null) {
                user.setEmpresa(loadCompany(cambios.getEmpresa()));
            }
            if (Boolean.FALSE.equals(cambios.getEstaActivo())) {
                user.setEstaActivo(false);
            }
            if (nuevaPassword != null && !nuevaPassword.isBlank()) {
                user.setPassword(BCrypt.hashpw(nuevaPassword, BCrypt.gensalt(12)));
                user.setIntentosFallidos(0);
            }
            if ("Funcionario de Empresa".equalsIgnoreCase(user.getRol().getNombreRol())
                    && user.getEmpresa() == null) {
                throw new IllegalArgumentException("El rol Funcionario de Empresa requiere una empresa.");
            }
            return usuarioRepository.save(user);
        });
    }

    public void desactivar(Integer id) {
        operations.execute("USUARIOS_ELIMINAR", "DESACTIVAR_USUARIO", "usuarios", id, () -> {
            Usuario user = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            user.setEstaActivo(false);
            usuarioRepository.save(user);
        });
    }

    public void activar(Integer id) {
        operations.execute("USUARIOS_EDITAR", "ACTIVAR_USUARIO", "usuarios", id, () -> {
            Usuario user = usuarioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
            user.setEstaActivo(true);
            user.setIntentosFallidos(0);
            usuarioRepository.save(user);
        });
    }

    private Empresa loadCompany(Empresa company) {
        if (company == null) {
            return null;
        }
        if (company.getId() == null) {
            throw new IllegalArgumentException("Empresa no válida.");
        }
        return empresaRepository.findById(company.getId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada."));
    }
}
