package com.acme.sica.repository;

import com.acme.sica.model.PuntoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PuntoAccesoRepository extends JpaRepository<PuntoAcceso, Integer> {
    Optional<PuntoAcceso> findByNombre(String nombre);
    List<PuntoAcceso> findByEstaActivoTrue();
}
