package com.acme.sica.repository;

import com.acme.sica.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {
    Optional<Empresa> findByNombre(String nombre);
    Optional<Empresa> findByNit(String nit);
    List<Empresa> findByEstaActivaTrue();
}
