package com.acme.sica.pattern.factory;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.repository.VisitaEstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PATRÓN FACTORY:
 * Centraliza la creación e inicialización de entidades Visita según el tipo de flujo de acceso,
 * asignando estados, códigos de pase únicos (UUID), flags temporales y metadatos correspondientes.
 */
@Component
@RequiredArgsConstructor
public class VisitaFactory {

    private final VisitaEstadoRepository visitaEstadoRepository;

    public Visita crearVisita(TipoFlujoAcceso tipoFlujo,
                             Persona persona,
                             Usuario anfitrion,
                             Usuario registradoPor,
                             PuntoAcceso puntoEntrada,
                             Empresa empresaDestino,
                             Vehiculo vehiculo,
                             AccesoRequestDto dto) {

        Visita.VisitaBuilder builder = Visita.builder()
                .persona(persona)
                .empresaDestino(empresaDestino != null ? empresaDestino : persona.getEmpresa())
                .anfitrion(anfitrion)
                .registradoPor(registradoPor)
                .puntoAccesoEntrada(puntoEntrada)
                .vehiculo(vehiculo)
                .vehiculoPlaca(dto != null && dto.getVehiculoPlaca() != null ? dto.getVehiculoPlaca().toUpperCase() : null)
                .motivoVisita(dto != null ? dto.getMotivoVisita() : null)
                .observaciones(dto != null ? dto.getObservaciones() : null);

        switch (tipoFlujo) {
            case INVITADO_PRE_REGISTRADO -> {
                VisitaEstado estadoAprobado = getEstado("Aprobado");
                builder.estadoVisita(estadoAprobado)
                       .esPaseTemporal(false)
                       .codigoPase(dto != null && dto.getCodigoPase() != null ? dto.getCodigoPase() : "PRE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                       .fechaProgramada(LocalDateTime.now());
            }

            case INVITADO_NO_ANUNCIADO -> {
                VisitaEstado estadoPendiente = getEstado("Pendiente de Aprobacion");
                builder.estadoVisita(estadoPendiente)
                       .esPaseTemporal(false)
                       .codigoPase("NOA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                       .fechaProgramada(LocalDateTime.now());
            }

            case CARNET_OLVIDADO -> {
                VisitaEstado estadoOlvido = getEstado("Pendiente de Aprobacion por Olvido");
                builder.estadoVisita(estadoOlvido)
                       .esPaseTemporal(true) // Bandera de pase temporal
                       .codigoPase("TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                       .fechaProgramada(LocalDateTime.now())
                       .motivoVisita("Olvido de Carnet Físico - Solicitud de Pase Temporal");
            }

            case SALIDA_OLVIDADA -> {
                VisitaEstado estadoDentro = getEstado("Dentro");
                builder.estadoVisita(estadoDentro)
                       .esPaseTemporal(false)
                       .codigoPase("REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                       .fechaEntrada(LocalDateTime.now())
                       .observaciones("Ingreso regularizado tras cierre automático de salida olvidada previa");
            }

            case TRABAJADOR_REGULAR -> {
                VisitaEstado estadoDentro = getEstado("Dentro");
                builder.estadoVisita(estadoDentro)
                       .esPaseTemporal(false)
                       .codigoPase("TRB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                       .fechaEntrada(LocalDateTime.now())
                       .motivoVisita("Jornada Laboral Regular");
            }

            default -> throw new IllegalArgumentException("Tipo de flujo de acceso desconocido: " + tipoFlujo);
        }

        return builder.build();
    }

    private VisitaEstado getEstado(String nombreEstado) {
        return visitaEstadoRepository.findByNombreEstado(nombreEstado)
                .orElseThrow(() -> new IllegalStateException("Estado de visita no configurado en BD: " + nombreEstado));
    }
}
