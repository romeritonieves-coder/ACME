package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VwPersonasActualmenteDentro {

    private Integer visitaId;

    private String documentoIdentidad;

    private String personaNombre;

    private String tipoPersona;

    private String empresaDestino;

    private String puntoEntrada;

    private LocalDateTime fechaEntrada;

    private String vehiculoPlaca;
}
