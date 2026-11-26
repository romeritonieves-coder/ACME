package com.acme.sica.service;

import com.acme.sica.dto.*;
import com.acme.sica.model.*;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.pattern.strategy.AccesoStrategyContext;
import com.acme.sica.repository.*;
import com.acme.sica.security.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AccesoService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccesoStrategyContext strategyContext;
    private final PersonaRepository personaRepository;
    private final VisitaRepository visitaRepository;
    private final PuntoAccesoRepository puntoAccesoRepository;
    private final VisitaEstadoRepository visitaEstadoRepository;
    private final VwPersonasActualmenteDentroRepository personasDentroRepository;
    private final VwSolicitudesPendientesRepository solicitudesPendientesRepository;
    private final AccesoSubject accesoSubject;
    private final AuditedOperationProxy operations;

    public AccesoService(AccesoStrategyContext strategyContext, PersonaRepository personaRepository,
                         VisitaRepository visitaRepository, PuntoAccesoRepository puntoAccesoRepository,
                         VisitaEstadoRepository visitaEstadoRepository,
                         VwPersonasActualmenteDentroRepository personasDentroRepository,
                         VwSolicitudesPendientesRepository solicitudesPendientesRepository,
                         AccesoSubject accesoSubject, AuditedOperationProxy operations) {
        this.strategyContext = strategyContext;
        this.personaRepository = personaRepository;
        this.visitaRepository = visitaRepository;
        this.puntoAccesoRepository = puntoAccesoRepository;
        this.visitaEstadoRepository = visitaEstadoRepository;
        this.personasDentroRepository = personasDentroRepository;
        this.solicitudesPendientesRepository = solicitudesPendientesRepository;
        this.accesoSubject = accesoSubject;
        this.operations = operations;
    }

    public AccesoResponseDto procesarFlujoAcceso(AccesoRequestDto request, Usuario usuarioOperador) {
        String permission = switch (request == null || request.getTipoFlujo() == null
                ? TipoFlujoAcceso.TRABAJADOR_REGULAR : request.getTipoFlujo()) {
            case INVITADO_NO_ANUNCIADO -> "ACCESO_SOLICITAR";
            case CARNET_OLVIDADO -> "ACCESO_PASE_TEMPORAL";
            default -> "ACCESO_CHECKIN";
        };
        return operations.executeAs(usuarioOperador, permission, "PROCESAR_FLUJO_ACCESO", "visitas", null, () -> {
            if (request == null || request.getDocumentoIdentidad() == null
                    || request.getDocumentoIdentidad().isBlank()) {
                throw new IllegalArgumentException("El documento de identidad es obligatorio.");
            }
            Persona persona = personaRepository.findByDocumentoIdentidad(request.getDocumentoIdentidad())
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona con documento "
                            + request.getDocumentoIdentidad() + "."));
            PuntoAcceso punto = request.getPuntoAccesoId() == null ? null
                    : puntoAccesoRepository.findById(request.getPuntoAccesoId())
                    .orElseThrow(() -> new IllegalArgumentException("Punto de acceso no encontrado."));
            TipoFlujoAcceso flow = request.getTipoFlujo() != null ? request.getTipoFlujo()
                    : ("Trabajador".equalsIgnoreCase(persona.getTipoPersona())
                    ? TipoFlujoAcceso.TRABAJADOR_REGULAR : TipoFlujoAcceso.INVITADO_PRE_REGISTRADO);
            List<Visita> active = visitaRepository.findVisitasActivasDentroPorPersona(persona.getId());
            if (!active.isEmpty() && flow != TipoFlujoAcceso.SALIDA_OLVIDADA) {
                flow = TipoFlujoAcceso.SALIDA_OLVIDADA;
            }
            if (flow == TipoFlujoAcceso.SALIDA_OLVIDADA) {
                if (active.isEmpty()) {
                    throw new IllegalStateException("No existe un ingreso activo que requiera regularización.");
                }
                SessionManager.getInstance().requirePermission("ACCESO_REGULARIZAR");
            }
            AccesoResponseDto response = strategyContext.ejecutarEstrategia(flow, request, persona, usuarioOperador, punto);
            return response;
        });
    }

    public AccesoResponseDto registrarSalida(Integer visitaId, Integer puntoAccesoSalidaId, Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "ACCESO_CHECKOUT", "CHECK_OUT", "visitas", visitaId, () -> {
            Visita visita = visitaRepository.findById(visitaId)
                    .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada con ID: " + visitaId));
            return registrarSalidaInterna(visita, puntoAccesoSalidaId);
        });
    }

    public AccesoResponseDto registrarSalidaPorDocumento(String documento, Integer puntoAccesoSalidaId,
                                                          Usuario usuarioOperador) {
        return operations.executeAs(usuarioOperador, "ACCESO_CHECKOUT", "CHECK_OUT_POR_DOCUMENTO", "visitas",
                null, () -> {
                    Persona person = personaRepository.findByDocumentoIdentidad(documento)
                            .orElseThrow(() -> new IllegalArgumentException("No se encontró la persona."));
                    Visita visit = visitaRepository.findVisitasActivasDentroPorPersona(person.getId()).stream()
                            .findFirst().orElseThrow(() ->
                                    new IllegalStateException("La persona no tiene un ingreso activo."));
                    return registrarSalidaInterna(visit, puntoAccesoSalidaId);
                });
    }

    public AccesoResponseDto resolverSolicitud(AprobacionRequestDto request, Usuario funcionario) {
        return operations.executeAs(funcionario, "ACCESO_APROBAR", "RESOLVER_SOLICITUD_VISITA", "visitas",
                request == null ? null : request.getVisitaId(), () -> {
                    if (request == null || request.getVisitaId() == null) {
                        throw new IllegalArgumentException("El identificador de visita es obligatorio.");
                    }
                    Visita visit = visitaRepository.findById(request.getVisitaId())
                            .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada."));
                    String currentState = visit.getEstadoVisita() == null
                            ? null : visit.getEstadoVisita().getNombreEstado();
                    if (!"Pendiente de Aprobacion".equals(currentState)
                            && !"Pendiente de Aprobacion por Olvido".equals(currentState)) {
                        throw new IllegalStateException("La solicitud ya fue resuelta o no está pendiente.");
                    }
                    String role = funcionario.getRol().getNombreRol();
                    if ("Funcionario de Empresa".equalsIgnoreCase(role)) {
                        Integer userCompanyId = funcionario.getEmpresa() == null
                                ? null : funcionario.getEmpresa().getId();
                        Integer visitCompanyId = visit.getEmpresaDestino() == null
                                ? null : visit.getEmpresaDestino().getId();
                        boolean isHost = visit.getAnfitrion() != null
                                && visit.getAnfitrion().getId().equals(funcionario.getId());
                        if (!isHost && (userCompanyId == null || !userCompanyId.equals(visitCompanyId))) {
                            throw new SecurityException("No puede aprobar solicitudes de otra empresa.");
                        }
                    }
                    String stateName = request.isAprobado() ? "Aprobado" : "Rechazado";
                    VisitaEstado state = visitaEstadoRepository.findByNombreEstado(stateName)
                            .orElseThrow(() -> new IllegalStateException("Estado no configurado: " + stateName));
                    visit.setEstadoVisita(state);
                    visit.setVisitaAprobadaPor(funcionario);
                    if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
                        String previous = visit.getObservaciones();
                        visit.setObservaciones((previous == null || previous.isBlank() ? "" : previous + " | ")
                                + request.getObservaciones());
                    }
                    visit = visitaRepository.save(visit);
                    Persona person = visit.getPersona();
                    String action = request.isAprobado() ? "APROBADA" : "RECHAZADA";
                    accesoSubject.notificarObservadores(AccesoEvento.builder().tipo("RESPUESTA_APROBACION")
                            .visitaId(visit.getId()).visita(visit).persona(person)
                            .destinatarioUsuarioId(visit.getRegistradoPor() == null
                                    ? null : visit.getRegistradoPor().getId())
                            .estadoVisita(stateName)
                            .mensaje("Solicitud para " + person.getNombre() + " " + action + " por "
                                    + funcionario.getNombre()).build());
                    return AccesoResponseDto.builder().exito(true).mensaje("Solicitud " + action + ".")
                            .visitaId(visit.getId()).estadoVisita(stateName).codigoPase(visit.getCodigoPase())
                            .personaNombre(person.getNombre()).documentoIdentidad(person.getDocumentoIdentidad())
                            .anfitrionNombre(funcionario.getNombre()).build();
                });
    }

    public List<EvacuacionPersonaDto> obtenerReporteEvacuacion() {
        SessionManager.getInstance().requirePermission("EVACUACION_VER");
        return personasDentroRepository.findAll().stream()
                        .map(row -> EvacuacionPersonaDto.builder().visitaId(row.getVisitaId())
                                .documentoIdentidad(row.getDocumentoIdentidad()).personaNombre(row.getPersonaNombre())
                                .tipoPersona(row.getTipoPersona())
                                .empresaDestino(row.getEmpresaDestino() == null ? "N/A" : row.getEmpresaDestino())
                                .puntoEntrada(row.getPuntoEntrada() == null ? "N/A" : row.getPuntoEntrada())
                                .fechaEntrada(row.getFechaEntrada() == null ? "N/A"
                                        : row.getFechaEntrada().format(FORMATTER))
                                .vehiculoPlaca(row.getVehiculoPlaca()).build())
                        .collect(Collectors.toList());
    }

    public Map<String, Long> obtenerMetricasEvacuacion() {
        SessionManager.getInstance().requirePermission("EVACUACION_VER");
        List<VwPersonasActualmenteDentro> rows = personasDentroRepository.findAll();
        return Map.of("totalDentro", (long) rows.size(),
                "trabajadoresDentro", rows.stream()
                        .filter(person -> "Trabajador".equalsIgnoreCase(person.getTipoPersona())).count(),
                "invitadosDentro", rows.stream()
                        .filter(person -> "Invitado".equalsIgnoreCase(person.getTipoPersona())).count());
    }

    public List<VwSolicitudesPendientes> obtenerSolicitudesPendientes(Usuario usuario) {
        SessionManager.getInstance().requirePermission("ACCESO_APROBAR");
        List<VwSolicitudesPendientes> requests = solicitudesPendientesRepository.findAll();
        if (usuario == null || usuario.getRol() == null
                || !"Funcionario de Empresa".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            return requests;
        }
        String company = usuario.getEmpresa() == null ? null : usuario.getEmpresa().getNombre();
        return requests.stream().filter(row -> row.getAnfitrion() == null
                || row.getAnfitrion().equalsIgnoreCase(usuario.getNombre())
                || (company != null && company.equalsIgnoreCase(row.getEmpresaDestino())))
                .collect(Collectors.toList());
    }

    private AccesoResponseDto registrarSalidaInterna(Visita visita, Integer puntoAccesoSalidaId) {
        VisitaEstado fuera = visitaEstadoRepository.findByNombreEstado("Fuera")
                .orElseThrow(() -> new IllegalStateException("Estado 'Fuera' no configurado en la base de datos."));
        PuntoAcceso point = puntoAccesoSalidaId == null ? null
                : puntoAccesoRepository.findById(puntoAccesoSalidaId)
                .orElseThrow(() -> new IllegalArgumentException("Punto de acceso no encontrado."));
        visita.setEstadoVisita(fuera);
        visita.setFechaSalida(java.time.LocalDateTime.now());
        visita.setPuntoAccesoSalida(point);
        visitaRepository.save(visita);
        Persona person = visita.getPersona();
        accesoSubject.notificarObservadores(AccesoEvento.builder().tipo("CHECK_OUT").visitaId(visita.getId())
                .visita(visita).persona(person).mensaje("Salida registrada para " + person.getNombre())
                .estadoVisita("Fuera").build());
        return AccesoResponseDto.builder().exito(true).mensaje("Salida registrada.")
                .visitaId(visita.getId()).estadoVisita("Fuera").personaNombre(person.getNombre())
                .documentoIdentidad(person.getDocumentoIdentidad()).tipoPersona(person.getTipoPersona())
                .urlFoto(person.getUrlFoto()).build();
    }

}
