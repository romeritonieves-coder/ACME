package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoResponseDto {
    private boolean exito;
    private String mensaje;
    private Integer visitaId;
    private String estadoVisita;
    private String codigoPase;
    private boolean requiereAprobacion;
    private boolean esPaseTemporal;
    private String personaNombre;
    private String documentoIdentidad;
    private String tipoPersona;
    private String urlFoto;
    private String anfitrionNombre;
    private String empresaNombre;
}
