package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoRequestDto {
    private TipoFlujoAcceso tipoFlujo;
    private String documentoIdentidad;
    private Integer puntoAccesoId;
    private String vehiculoPlaca;
    private Integer empresaDestinoId;
    private Integer anfitrionId;
    private String motivoVisita;
    private String observaciones;
    private String codigoPase;
}
