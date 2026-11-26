package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.repository.VisitaEstadoRepository;
import com.acme.sica.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FLUJO 1: Invitado pre-registrado
 * Check-in inmediato validando que la visita esté pre-aprobada, mostrando foto y anfitrión.
 */
@RequiredArgsConstructor
public class InvitadoPreRegistradoStrategy implements AccesoStrategy {

    private final VisitaRepository visitaRepository;
    private final VisitaEstadoRepository visitaEstadoRepository;
    private final VisitaFactory visitaFactory;
    private final AccesoSubject accesoSubject;

    @Override
    public boolean aplicaPara(TipoFlujoAcceso tipoFlujo) {
        return TipoFlujoAcceso.INVITADO_PRE_REGISTRADO.equals(tipoFlujo);
    }

    @Override
    public AccesoResponseDto procesar(AccesoRequestDto request, Persona persona, Usuario usuarioActual, PuntoAcceso puntoAcceso) {
        // Validar prohibición de ingreso
        if (persona.getEstadoAcceso() == null
                || !"Activo".equalsIgnoreCase(persona.getEstadoAcceso().getNombreEstado())) {
            return AccesoResponseDto.builder()
                    .exito(false)
                    .mensaje("ACCESO DENEGADO: La persona tiene restricción de seguridad: " + persona.getMotivoRestriccion())
                    .personaNombre(persona.getNombre())
                    .documentoIdentidad(persona.getDocumentoIdentidad())
                    .urlFoto(persona.getUrlFoto())
                    .build();
        }

        // Buscar visita pre-registrada aprobada
        List<Visita> aprobadas = visitaRepository.findVisitasAprobadasPorPersona(persona.getId());
        Visita visita;
        VisitaEstado estadoDentro = visitaEstadoRepository.findByNombreEstado("Dentro")
                .orElseThrow(() -> new IllegalStateException("Estado 'Dentro' no existe"));

        if (!aprobadas.isEmpty()) {
            visita = aprobadas.get(0);
        } else {
            // Si viene con anfitrión o pre-registro directo
            visita = visitaFactory.crearVisita(
                    TipoFlujoAcceso.INVITADO_PRE_REGISTRADO,
                    persona,
                    null,
                    usuarioActual,
                    puntoAcceso,
                    persona.getEmpresa(),
                    null,
                    request
            );
        }

        // Marcar check-in efectivo
        visita.setEstadoVisita(estadoDentro);
        visita.setFechaEntrada(LocalDateTime.now());
        visita.setPuntoAccesoEntrada(puntoAcceso);
        if (request.getVehiculoPlaca() != null && !request.getVehiculoPlaca().isBlank()) {
            visita.setVehiculoPlaca(request.getVehiculoPlaca().toUpperCase());
        }
        visita = visitaRepository.save(visita);

        String anfitrionNombre = visita.getAnfitrion() != null ? visita.getAnfitrion().getNombre() : "No especificado";
        String empresaNombre = visita.getEmpresaDestino() != null ? visita.getEmpresaDestino().getNombre() : (persona.getEmpresa() != null ? persona.getEmpresa().getNombre() : "Complejo Acme");

        // Publish an access event for desktop views and the evacuation census.
        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("CHECK_IN")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .mensaje("Check-in exitoso de invitado pre-registrado: " + persona.getNombre())
                .estadoVisita("Dentro")
                .destinatarioUsuarioId(visita.getAnfitrion() != null ? visita.getAnfitrion().getId() : null)
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Check-in exitoso. Invitado autorizado a ingresar.")
                .visitaId(visita.getId())
                .estadoVisita("Dentro")
                .codigoPase(visita.getCodigoPase())
                .requiereAprobacion(false)
                .esPaseTemporal(false)
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .anfitrionNombre(anfitrionNombre)
                .empresaNombre(empresaNombre)
                .build();
    }
}
