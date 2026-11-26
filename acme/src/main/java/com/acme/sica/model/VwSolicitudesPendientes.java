package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VwSolicitudesPendientes {

    private Integer visitaId;

    private String visitante;

    private String documentoIdentidad;

    private String tipoPersona;

    private String estado;

    private String empresaDestino;

    private String anfitrion;

    private Boolean esPaseTemporal;

    private LocalDateTime fechaSolicitud;
}
