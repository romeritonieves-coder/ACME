package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "visita_id", nullable = false)
    private Visita visita;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reportado_por_id", nullable = false)
    private Usuario reportadoPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_acceso_id")
    private PuntoAcceso puntoAcceso;

    @Column(name = "fecha", nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "severidad", nullable = false, length = 20)
    @Builder.Default
    private String severidad = "Media"; // Baja, Media, Alta, Critica

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "Abierto"; // Abierto, En Investigacion, Cerrado

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "acciones_tomadas", columnDefinition = "TEXT")
    private String accionesTomadas;
}
