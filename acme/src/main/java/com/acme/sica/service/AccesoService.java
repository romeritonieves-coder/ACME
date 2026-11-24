package com.acme.sica.service;

import com.acme.sica.dto.*;
import com.acme.sica.model.*;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.pattern.proxy.Auditable;
import com.acme.sica.pattern.strategy.AccesoStrategyContext;
import com.acme.sica.repository.*;
import com.acme.sica.security.RequirePermiso;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccesoService {

    private final AccesoStrategyContext strategyContext;
    private final PersonaRepository personaRepository;
    private final VisitaRepository visitaRepository;
    private final PuntoAccesoRepository puntoAccesoRepository;
    private final VisitaEstadoRepository visitaEstadoRepository;
    private final VwPersonasActualmenteDentroRepository personasDentroRepository;
    private final VwSolicitudesPendientesRepository solicitudesPendientesRepository;
    private final AccesoSubject accesoSubject;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Procesa la solicitud de acceso ejecutando la estrategia correspondiente
     * de los 4 flujos obligatorios de SICA.
     */
    @Transactional
    @RequirePermiso("ACCESO_CHECKIN")
    @Auditable(accion = "PROCESAR_FLUJO_ACCESO", tabla = "visitas")
    public AccesoResponseDto procesarFlujoAcceso(AccesoRequestDto request, Usuario usuarioOperador) {
        Persona persona = personaRepository.findByDocumentoIdentidad(request.getDocumentoIdentidad())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ninguna persona registrada con documento: " + request.getDocumentoIdentidad()));

        PuntoAcceso punto = null;
        if (request.getPuntoAccesoId() != null) {
            punto = puntoAccesoRepository.findById(request.getPuntoAccesoId()).orElse(null);
        }

        TipoFlujoAcceso flujo = request.getTipoFlujo() != null ? request.getTipoFlujo() :
                ("Trabajador".equalsIgnoreCase(persona.getTipoPersona()) ? TipoFlujoAcceso.TRABAJADOR_REGULAR : TipoFlujoAcceso.INVITADO_PRE_REGISTRADO);

        // Si la persona ya tiene visita activa en estado 'Dentro' y solicita entrar sin flujo de salida olvidada,
        // se activa automáticamente la regularización de salida olvidada (Flujo 4)
        List<Visita> activas = visitaRepository.findVisitasActivasDentroPorPersona(persona.getId());
        if (!activas.isEmpty() && flujo != TipoFlujoAcceso.SALIDA_OLVIDADA) {
            log.warn("Detectada visita activa previa sin salida para {}. Activando automáticamente flujo SALIDA_OLVIDADA.", persona.getNombre());
            flujo = TipoFlujoAcceso.SALIDA_OLVIDADA;
        }

        return strategyContext.ejecutarEstrategia(flujo, request, persona, usuarioOperador, punto);
    }

    /**
     * Registro de Check-Out (Salida física del complejo)
     */
    @Transactional
    @RequirePermiso("ACCESO_CHECKOUT")
    @Auditable(accion = "CHECK_OUT", tabla = "visitas")
    public AccesoResponseDto registrarSalida(Integer visitaId, Integer puntoAccesoSalidaId, Usuario usuarioOperador) {
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada con ID: " + visitaId));

        VisitaEstado estadoFuera = visitaEstadoRepository.findByNombreEstado("Fuera")
                .orElseThrow(() -> new IllegalStateException("Estado 'Fuera' no configurado"));

        PuntoAcceso puntoSalida = null;
        if (puntoAccesoSalidaId != null) {
            puntoSalida = puntoAccesoRepository.findById(puntoAccesoSalidaId).orElse(null);
        }

        visita.setEstadoVisita(estadoFuera);
        visita.setFechaSalida(LocalDateTime.now());
        visita.setPuntoAccesoSalida(puntoSalida);
        visita = visitaRepository.save(visita);

        Persona persona = visita.getPersona();

        // Notificar por Observer a portería y censo de evacuación
        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("CHECK_OUT")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .mensaje("Salida registrada con éxito para: " + persona.getNombre())
                .estadoVisita("Fuera")
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Salida registrada exitosamente.")
                .visitaId(visita.getId())
                .estadoVisita("Fuera")
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .build();
    }

    /**
     * Aprobación o Rechazo en tiempo real por Funcionario de Empresa (o Supervisor)
     */
    @Transactional
    @RequirePermiso("ACCESO_APROBAR")
    @Auditable(accion = "RESOLVER_SOLICITUD_VISITA", tabla = "visitas")
    public AccesoResponseDto resolverSolicitud(AprobacionRequestDto request, Usuario funcionario) {
        Visita visita = visitaRepository.findById(request.getVisitaId())
                .orElseThrow(() -> new IllegalArgumentException("Visita no encontrada con ID: " + request.getVisitaId()));

        String nuevoEstadoNombre = request.isAprobado() ? "Aprobado" : "Rechazado";
        VisitaEstado nuevoEstado = visitaEstadoRepository.findByNombreEstado(nuevoEstadoNombre)
                .orElseThrow(() -> new IllegalStateException("Estado no configurado: " + nuevoEstadoNombre));

        visita.setEstadoVisita(nuevoEstado);
        visita.setVisitaAprobadaPor(funcionario);
        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            visita.setObservaciones((visita.getObservaciones() != null ? visita.getObservaciones() + " | " : "") +
                    request.getObservaciones());
        }

        visita = visitaRepository.save(visita);
        Persona persona = visita.getPersona();

        // Notificar en tiempo real al Guarda vía WebSocket (Observer)
        String accionTexto = request.isAprobado() ? "APROBADA" : "RECHAZADA";
        String mensaje = String.format("La solicitud de ingreso para %s (Doc: %s) ha sido %s por %s",
                persona.getNombre(), persona.getDocumentoIdentidad(), accionTexto, funcionario.getNombre());

        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("RESPUESTA_APROBACION")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .mensaje(mensaje)
                .estadoVisita(nuevoEstadoNombre)
                .destinatarioUsuarioId(visita.getRegistradoPor() != null ? visita.getRegistradoPor().getId() : null)
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Solicitud " + accionTexto.toLowerCase() + " con éxito.")
                .visitaId(visita.getId())
                .estadoVisita(nuevoEstadoNombre)
                .codigoPase(visita.getCodigoPase())
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .anfitrionNombre(funcionario.getNombre())
                .build();
    }

    /**
     * Reporte en tiempo real de censo de personas dentro del complejo para Evacuación y Emergencias
     * Utiliza directamente la vista SQL `vw_personas_actualmente_dentro` y Streams API.
     */
    @Transactional(readOnly = true)
    @RequirePermiso("EVACUACION_VER")
    public List<EvacuacionPersonaDto> obtenerReporteEvacuacion() {
        return personasDentroRepository.findAll().stream()
                .map(v -> EvacuacionPersonaDto.builder()
                        .visitaId(v.getVisitaId())
                        .documentoIdentidad(v.getDocumentoIdentidad())
                        .personaNombre(v.getPersonaNombre())
                        .tipoPersona(v.getTipoPersona())
                        .empresaDestino(v.getEmpresaDestino() != null ? v.getEmpresaDestino() : "N/A")
                        .puntoEntrada(v.getPuntoEntrada() != null ? v.getPuntoEntrada() : "N/A")
                        .fechaEntrada(v.getFechaEntrada() != null ? v.getFechaEntrada().format(FORMATTER) : "N/A")
                        .vehiculoPlaca(v.getVehiculoPlaca())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Métricas de Evacuación (Total dentro, trabajadores dentro, invitados dentro)
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obtenerMetricasEvacuacion() {
        List<VwPersonasActualmenteDentro> lista = personasDentroRepository.findAll();
        long trabajadores = lista.stream().filter(p -> "Trabajador".equalsIgnoreCase(p.getTipoPersona())).count();
        long invitados = lista.stream().filter(p -> "Invitado".equalsIgnoreCase(p.getTipoPersona())).count();

        return Map.of(
                "totalDentro", (long) lista.size(),
                "trabajadoresDentro", trabajadores,
                "invitadosDentro", invitados
        );
    }

    /**
     * Obtiene solicitudes pendientes de aprobación (desde la vista o filtro por anfitrión)
     */
    @Transactional(readOnly = true)
    public List<VwSolicitudesPendientes> obtenerSolicitudesPendientes(Usuario usuario) {
        if (usuario != null && usuario.getRol() != null && "Funcionario de Empresa".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            // Filtrar las que le corresponden a él
            return solicitudesPendientesRepository.findAll().stream()
                    .filter(s -> s.getAnfitrion() == null || s.getAnfitrion().equalsIgnoreCase(usuario.getNombre()) || (usuario.getEmpresa() != null && usuario.getEmpresa().getNombre().equalsIgnoreCase(s.getEmpresaDestino())))
                    .collect(Collectors.toList());
        }
        return solicitudesPendientesRepository.findAll();
    }
}
