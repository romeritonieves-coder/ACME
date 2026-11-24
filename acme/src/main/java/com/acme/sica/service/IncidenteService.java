package com.acme.sica.service;

import com.acme.sica.dto.IncidenteDto;
import com.acme.sica.model.*;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.pattern.proxy.Auditable;
import com.acme.sica.repository.IncidenteRepository;
import com.acme.sica.repository.PuntoAccesoRepository;
import com.acme.sica.repository.VisitaRepository;
import com.acme.sica.security.RequirePermiso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidenteService {

    private final IncidenteRepository incidenteRepository;
    private final VisitaRepository visitaRepository;
    private final PuntoAccesoRepository puntoAccesoRepository;
    private final AccesoSubject accesoSubject;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    @RequirePermiso("INCIDENTES_VER")
    public List<IncidenteDto> listarTodos() {
        return incidenteRepository.findAllByOrderByFechaDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @RequirePermiso("INCIDENTES_REGISTRAR")
    @Auditable(accion = "REGISTRAR_INCIDENTE", tabla = "incidentes")
    public IncidenteDto registrarIncidente(IncidenteDto dto, Usuario usuarioOperador) {
        Visita visita = visitaRepository.findById(dto.getVisitaId())
                .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada con ID: " + dto.getVisitaId()));

        PuntoAcceso punto = null;
        if (dto.getPuntoAccesoId() != null) {
            punto = puntoAccesoRepository.findById(dto.getPuntoAccesoId()).orElse(null);
        }

        Incidente incidente = Incidente.builder()
                .visita(visita)
                .reportadoPor(usuarioOperador)
                .puntoAcceso(punto)
                .fecha(LocalDateTime.now())
                .severidad(dto.getSeveridad() != null ? dto.getSeveridad() : "Media")
                .estado("Abierto")
                .descripcion(dto.getDescripcion())
                .accionesTomadas(dto.getAccionesTomadas())
                .build();

        incidente = incidenteRepository.save(incidente);

        // Notificar alerta por WebSocket a supervisores y portería
        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("ALERTA_SEGURIDAD")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(visita.getPersona())
                .mensaje(String.format("Incidente reportado [%s]: %s (Severidad: %s)",
                        incidente.getId(), incidente.getDescripcion(), incidente.getSeveridad()))
                .build());

        return mapToDto(incidente);
    }

    @Transactional
    @RequirePermiso("INCIDENTES_GESTIONAR")
    @Auditable(accion = "ACTUALIZAR_INCIDENTE", tabla = "incidentes")
    public IncidenteDto actualizarEstado(Integer incidenteId, String nuevoEstado, String accionesTomadas, Usuario usuarioOperador) {
        Incidente incidente = incidenteRepository.findById(incidenteId)
                .orElseThrow(() -> new IllegalArgumentException("Incidente no encontrado con ID: " + incidenteId));

        incidente.setEstado(nuevoEstado);
        if (accionesTomadas != null && !accionesTomadas.isBlank()) {
            incidente.setAccionesTomadas(accionesTomadas);
        }
        incidente = incidenteRepository.save(incidente);

        return mapToDto(incidente);
    }

    private IncidenteDto mapToDto(Incidente i) {
        return IncidenteDto.builder()
                .id(i.getId())
                .visitaId(i.getVisita().getId())
                .puntoAccesoId(i.getPuntoAcceso() != null ? i.getPuntoAcceso().getId() : null)
                .severidad(i.getSeveridad())
                .estado(i.getEstado())
                .descripcion(i.getDescripcion())
                .accionesTomadas(i.getAccionesTomadas())
                .reportadoPorNombre(i.getReportadoPor() != null ? i.getReportadoPor().getNombre() : "N/A")
                .fecha(i.getFecha() != null ? i.getFecha().format(FORMATTER) : "N/A")
                .build();
    }
}
