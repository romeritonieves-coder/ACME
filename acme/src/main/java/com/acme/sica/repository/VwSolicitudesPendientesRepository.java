package com.acme.sica.repository;

import com.acme.sica.model.VwSolicitudesPendientes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VwSolicitudesPendientesRepository extends JpaRepository<VwSolicitudesPendientes, Integer> {
    List<VwSolicitudesPendientes> findAll();
}
