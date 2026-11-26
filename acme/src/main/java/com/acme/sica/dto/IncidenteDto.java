package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidenteDto {
    private Integer id;

    private Integer visitaId;

    private Integer puntoAccesoId;
    private String severidad; // Baja, Media, Alta, Critica
    private String estado;    // Abierto, En Investigacion, Cerrado

    private String descripcion;

    private String accionesTomadas;
    private String reportadoPorNombre;
    private String fecha;
}
