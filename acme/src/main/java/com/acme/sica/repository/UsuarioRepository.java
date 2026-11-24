package com.acme.sica.repository;

import com.acme.sica.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRolId(Integer rolId);
    List<Usuario> findByEmpresaId(Integer empresaId);
    List<Usuario> findByEstaActivoTrue();
}
