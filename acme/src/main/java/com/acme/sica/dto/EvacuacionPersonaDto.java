package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvacuacionPersonaDto {
    private Integer visitaId;
    private String documentoIdentidad;
    private String personaNombre;
    private String tipoPersona;
    private String empresaDestino;
    private String puntoEntrada;
    private String fechaEntrada;
    private String vehiculoPlaca;
}
