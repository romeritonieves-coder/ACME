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
import java.util.logging.Logger;

/**
 * FLUJO 4: Regularización de salida olvidada
 * Detecta si la persona tiene una visita previa en estado 'Dentro' sin salida registrada.
 * Cierra automáticamente la visita anterior como 'Cerrada por Sistema (Salida Olvidada)'
 * y abre de inmediato la nueva visita en estado 'Dentro'.
 */
@RequiredArgsConstructor
public class SalidaOlvidadaStrategy implements AccesoStrategy {
    private static final Logger LOGGER = Logger.getLogger(SalidaOlvidadaStrategy.class.getName());

    private final VisitaRepository visitaRepository;
    private final VisitaEstadoRepository visitaEstadoRepository;
    private final VisitaFactory visitaFactory;
    private final AccesoSubject accesoSubject;

    @Override
    public boolean aplicaPara(TipoFlujoAcceso tipoFlujo) {
        return TipoFlujoAcceso.SALIDA_OLVIDADA.equals(tipoFlujo);
    }

    @Override
    public AccesoResponseDto procesar(AccesoRequestDto request, Persona persona, Usuario usuarioActual, PuntoAcceso puntoAcceso) {
        // Validar prohibición de ingreso
        if (persona.getEstadoAcceso() == null
                || !"Activo".equalsIgnoreCase(persona.getEstadoAcceso().getNombreEstado())) {
            return AccesoResponseDto.builder()
                    .exito(false)
                    .mensaje("ACCESO DENEGADO: La persona tiene prohibición de ingreso: " + persona.getMotivoRestriccion())
                    .personaNombre(persona.getNombre())
                    .documentoIdentidad(persona.getDocumentoIdentidad())
                    .urlFoto(persona.getUrlFoto())
                    .build();
        }

        VisitaEstado estadoCerradaOlvido = visitaEstadoRepository.findByNombreEstado("Cerrada por Sistema (Salida Olvidada)")
                .orElseThrow(() -> new IllegalStateException("Estado 'Cerrada por Sistema (Salida Olvidada)' no configurado"));

        // 1. Cerrar automáticamente la visita previa abierta
        List<Visita> visitasActivas = visitaRepository.findVisitasActivasDentroPorPersona(persona.getId());
        for (Visita anterior : visitasActivas) {
            anterior.setEstadoVisita(estadoCerradaOlvido);
            anterior.setFechaSalida(LocalDateTime.now());
            anterior.setPuntoAccesoSalida(puntoAcceso);
            anterior.setObservaciones((anterior.getObservaciones() != null ? anterior.getObservaciones() + " | " : "") +
                    "Cerrada automáticamente por regularización del sistema al registrar nuevo acceso");
            visitaRepository.save(anterior);
            LOGGER.info(() -> "Previous visit closed after missed checkout: " + anterior.getId());
        }

        // 2. Crear y abrir la nueva visita directamente en estado 'Dentro'
        Visita nuevaVisita = visitaFactory.crearVisita(
                TipoFlujoAcceso.SALIDA_OLVIDADA,
                persona,
                null,
                usuarioActual,
                puntoAcceso,
                persona.getEmpresa(),
                null,
                request
        );

        nuevaVisita = visitaRepository.save(nuevaVisita);

        // Notificar a portería y censo de evacuación
        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("REGULARIZACION")
                .visitaId(nuevaVisita.getId())
                .visita(nuevaVisita)
                .persona(persona)
                .mensaje("Regularización de salida olvidada completada para: " + persona.getNombre())
                .estadoVisita("Dentro")
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Salida previa regularizada y cerrada por el sistema. Nuevo ingreso registrado con éxito.")
                .visitaId(nuevaVisita.getId())
                .estadoVisita("Dentro")
                .codigoPase(nuevaVisita.getCodigoPase())
                .requiereAprobacion(false)
                .esPaseTemporal(false)
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .anfitrionNombre(persona.getEmpresa() != null ? persona.getEmpresa().getNombre() : "Empresa Registrada")
                .empresaNombre(persona.getEmpresa() != null ? persona.getEmpresa().getNombre() : "Complejo Acme")
                .build();
    }
}
