package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_destino_id")
    private Empresa empresaDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "anfitrion_id")
    private Usuario anfitrion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visita_aprobada_por")
    private Usuario visitaAprobadaPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_acceso_entrada_id")
    private PuntoAcceso puntoAccesoEntrada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_acceso_salida_id")
    private PuntoAcceso puntoAccesoSalida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @Column(name = "vehiculo_placa", length = 10)
    private String vehiculoPlaca;

    @Column(name = "codigo_pase", unique = true, length = 36)
    private String codigoPase;

    @Column(name = "es_pase_temporal", nullable = false)
    @Builder.Default
    private Boolean esPaseTemporal = false;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_entrada")
    private LocalDateTime fechaEntrada;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_visita_id", nullable = false)
    private VisitaEstado estadoVisita;

    @Column(name = "motivo_visita", length = 255)
    private String motivoVisita;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
