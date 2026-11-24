package com.acme.sica.repository;

import com.acme.sica.model.VwPersonasActualmenteDentro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VwPersonasActualmenteDentroRepository extends JpaRepository<VwPersonasActualmenteDentro, Integer> {
    List<VwPersonasActualmenteDentro> findAll();
    long countByTipoPersona(String tipoPersona);
}
