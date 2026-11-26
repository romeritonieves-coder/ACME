package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {

    private Integer id;

    private Persona persona;

    private Empresa empresaDestino;

    private Usuario anfitrion;

    private Usuario registradoPor;

    private Usuario visitaAprobadaPor;

    private PuntoAcceso puntoAccesoEntrada;

    private PuntoAcceso puntoAccesoSalida;

    private Vehiculo vehiculo;

    private String vehiculoPlaca;

    private String codigoPase;

    @Builder.Default
    private Boolean esPaseTemporal = false;

    private LocalDateTime fechaProgramada;

    private LocalDateTime fechaEntrada;

    private LocalDateTime fechaSalida;

    private VisitaEstado estadoVisita;

    private String motivoVisita;

    private String observaciones;

    private LocalDateTime createdAt;
}
