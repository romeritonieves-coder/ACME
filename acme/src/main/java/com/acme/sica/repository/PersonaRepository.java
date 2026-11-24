package com.acme.sica.repository;

import com.acme.sica.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    Optional<Persona> findByDocumentoIdentidad(String documentoIdentidad);
    List<Persona> findByEmpresaId(Integer empresaId);
    List<Persona> findByTipoPersona(String tipoPersona);

    @Query("SELECT p FROM Persona p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR p.documentoIdentidad LIKE CONCAT('%', :query, '%')")
    List<Persona> buscarPorNombreODocumento(@Param("query") String query);
}
