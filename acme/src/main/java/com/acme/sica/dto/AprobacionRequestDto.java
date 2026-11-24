package com.acme.sica.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobacionRequestDto {
    @NotNull(message = "El ID de la visita es obligatorio")
    private Integer visitaId;

    private boolean aprobado;
    private String observaciones;
}
