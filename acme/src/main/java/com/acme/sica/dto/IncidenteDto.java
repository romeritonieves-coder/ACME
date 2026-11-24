package com.acme.sica.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidenteDto {
    private Integer id;

    @NotNull(message = "El ID de la visita es obligatorio")
    private Integer visitaId;

    private Integer puntoAccesoId;
    private String severidad; // Baja, Media, Alta, Critica
    private String estado;    // Abierto, En Investigacion, Cerrado

    @NotBlank(message = "La descripción es requerida")
    private String descripcion;

    private String accionesTomadas;
    private String reportadoPorNombre;
    private String fecha;
}
