package com.acme.sica.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    private Integer id;

    private String modulo;

    private String nombrePermiso;

    private String descripcion;
}
