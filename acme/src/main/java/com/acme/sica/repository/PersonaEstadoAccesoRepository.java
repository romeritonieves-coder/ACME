package com.acme.sica.repository;

import com.acme.sica.model.PersonaEstadoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PersonaEstadoAccesoRepository extends JpaRepository<PersonaEstadoAcceso, Integer> {
    Optional<PersonaEstadoAcceso> findByNombreEstado(String nombreEstado);
}
