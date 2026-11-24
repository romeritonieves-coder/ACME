package com.acme.sica.repository;

import com.acme.sica.model.Incidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidenteRepository extends JpaRepository<Incidente, Integer> {
    List<Incidente> findAllByOrderByFechaDesc();
    List<Incidente> findByEstado(String estado);
    List<Incidente> findBySeveridad(String severidad);
}
