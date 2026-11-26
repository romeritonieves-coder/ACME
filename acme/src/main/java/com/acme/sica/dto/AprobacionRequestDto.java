package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AprobacionRequestDto {
    private Integer visitaId;

    private boolean aprobado;
    private String observaciones;
}
