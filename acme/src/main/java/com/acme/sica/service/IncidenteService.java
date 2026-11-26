package com.acme.sica.service;

import com.acme.sica.dto.IncidenteDto;
import com.acme.sica.model.Incidente;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;
import com.acme.sica.model.Visita;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.repository.IncidenteRepository;
import com.acme.sica.repository.PuntoAccesoRepository;
import com.acme.sica.repository.VisitaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IncidenteService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IncidenteRepository incidenteRepository;
    private final VisitaRepository visitaRepository;
    private final PuntoAccesoRepository puntoAccesoRepository;
    private final AccesoSubject accesoSubject;
    private final AuditedOperationProxy operations;

    public IncidenteService(IncidenteRepository incidenteRepository, VisitaRepository visitaRepository,
                            PuntoAccesoRepository puntoAccesoRepository, AccesoSubject accesoSubject,
                            AuditedOperationProxy operations) {
        this.incidenteRepository = incidenteRepository;
        this.visitaRepository = visitaRepository;
        this.puntoAccesoRepository = puntoAccesoRepository;
        this.accesoSubject = accesoSubject;
        this.operations = operations;
    }

    public List<IncidenteDto> listarTodos() {
        return operations.execute("INCIDENTES_VER", "CONSULTAR_INCIDENTES", "incidentes", null,
                () -> incidenteRepository.findAllByOrderByFechaDesc().stream().map(this::mapToDto).toList());
    }

    public IncidenteDto registrarIncidente(IncidenteDto dto, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "INCIDENTES_REGISTRAR", "REGISTRAR_INCIDENTE", "incidentes",
                dto == null ? null : dto.getId(), () -> {
            if (dto == null || dto.getVisitaId() == null || dto.getDescripcion() == null
                    || dto.getDescripcion().isBlank()) {
                throw new IllegalArgumentException("Visita y descripción del incidente son obligatorias.");
            }
            Visita visit = visitaRepository.findById(dto.getVisitaId())
                    .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada."));
            PuntoAcceso point = dto.getPuntoAccesoId() == null ? null
                    : puntoAccesoRepository.findById(dto.getPuntoAccesoId())
                    .orElseThrow(() -> new IllegalArgumentException("Punto de acceso no encontrado."));
            Incidente incident = incidenteRepository.save(Incidente.builder().visita(visit)
                    .reportadoPor(usuarioOperador).puntoAcceso(point).fecha(LocalDateTime.now())
                    .severidad(dto.getSeveridad() == null ? "Media" : dto.getSeveridad())
                    .estado("Abierto").descripcion(dto.getDescripcion()).accionesTomadas(dto.getAccionesTomadas())
                    .build());
            accesoSubject.notificarObservadores(AccesoEvento.builder().tipo("ALERTA_SEGURIDAD")
                    .visitaId(visit.getId()).visita(visit).persona(visit.getPersona())
                    .mensaje("Incidente " + incident.getId() + ": " + incident.getDescripcion()).build());
            return mapToDto(incident);
        });
    }

    public IncidenteDto actualizarEstado(Integer incidenteId, String nuevoEstado,
                                         String accionesTomadas, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "INCIDENTES_GESTIONAR", "ACTUALIZAR_INCIDENTE", "incidentes",
                incidenteId, () -> {
                    Incidente incident = incidenteRepository.findById(incidenteId)
                            .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado."));
                    incident.setEstado(nuevoEstado);
                    if (accionesTomadas != null && !accionesTomadas.isBlank()) {
                        incident.setAccionesTomadas(accionesTomadas);
                    }
                    return mapToDto(incidenteRepository.save(incident));
                });
    }

    public void eliminarIncidente(Integer incidenteId, Usuario usuarioOperador) {
        operations.executeAs(usuarioOperador, "INCIDENTES_GESTIONAR", "ELIMINAR_INCIDENTE", "incidentes", incidenteId,
                () -> incidenteRepository.deleteById(incidenteId));
    }

    private IncidenteDto mapToDto(Incidente incident) {
        Visita visit = incident.getVisita();
        return IncidenteDto.builder().id(incident.getId()).visitaId(visit == null ? null : visit.getId())
                .puntoAccesoId(incident.getPuntoAcceso() == null ? null : incident.getPuntoAcceso().getId())
                .severidad(incident.getSeveridad()).estado(incident.getEstado())
                .descripcion(incident.getDescripcion()).accionesTomadas(incident.getAccionesTomadas())
                .reportadoPorNombre(incident.getReportadoPor() == null ? "N/A" : incident.getReportadoPor().getNombre())
                .fecha(incident.getFecha() == null ? "N/A" : incident.getFecha().format(FORMATTER)).build();
    }
}
