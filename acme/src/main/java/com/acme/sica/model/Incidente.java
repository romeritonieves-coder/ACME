package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidente {

    private Integer id;

    private Visita visita;

    private Usuario reportadoPor;

    private PuntoAcceso puntoAcceso;

    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

    @Builder.Default
    private String severidad = "Media"; // Baja, Media, Alta, Critica

    @Builder.Default
    private String estado = "Abierto"; // Abierto, En Investigacion, Cerrado

    private String descripcion;

    private String accionesTomadas;
}
