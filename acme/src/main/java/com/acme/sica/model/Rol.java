package com.acme.sica.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    private Integer id;

    private String nombreRol;

    private String descripcion;
}
