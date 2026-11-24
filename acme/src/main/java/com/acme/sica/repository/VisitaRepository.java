package com.acme.sica.repository;

import com.acme.sica.model.Visita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitaRepository extends JpaRepository<Visita, Integer> {
    Optional<Visita> findByCodigoPase(String codigoPase);

    List<Visita> findByPersonaIdOrderByCreatedAtDesc(Integer personaId);

    // Encuentra si la persona tiene una visita activa en estado 'Dentro'
    @Query("SELECT v FROM Visita v WHERE v.persona.id = :personaId AND v.estadoVisita.nombreEstado = 'Dentro' ORDER BY v.fechaEntrada DESC")
    List<Visita> findVisitasActivasDentroPorPersona(@Param("personaId") Integer personaId);

    // Encuentra la última visita pendiente o aprobada para check-in
    @Query("SELECT v FROM Visita v WHERE v.persona.id = :personaId AND v.estadoVisita.nombreEstado = 'Aprobado' ORDER BY v.id DESC")
    List<Visita> findVisitasAprobadasPorPersona(@Param("personaId") Integer personaId);

    // Solicitudes pendientes de aprobación
    @Query("SELECT v FROM Visita v WHERE v.estadoVisita.nombreEstado IN ('Pendiente de Aprobacion', 'Pendiente de Aprobacion por Olvido') ORDER BY v.createdAt DESC")
    List<Visita> findSolicitudesPendientes();

    // Solicitudes pendientes dirigidas a un anfitrión específico
    @Query("SELECT v FROM Visita v WHERE v.anfitrion.id = :anfitrionId AND v.estadoVisita.nombreEstado IN ('Pendiente de Aprobacion', 'Pendiente de Aprobacion por Olvido') ORDER BY v.createdAt DESC")
    List<Visita> findSolicitudesPendientesPorAnfitrion(@Param("anfitrionId") Integer anfitrionId);
}
