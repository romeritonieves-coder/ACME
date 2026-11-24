package com.acme.sica.repository;

import com.acme.sica.model.VisitaEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VisitaEstadoRepository extends JpaRepository<VisitaEstado, Integer> {
    Optional<VisitaEstado> findByNombreEstado(String nombreEstado);
}
