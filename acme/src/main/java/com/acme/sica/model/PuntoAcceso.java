package com.acme.sica.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoAcceso {

    private Integer id;

    private String nombre;

    private String tipo; // Peatonal, Vehicular, Mixto

    @Builder.Default
    private Boolean estaActivo = true;
}
