package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    private Integer id;

    private String nombre;

    private String nit;

    private String ubicacion;

    private String contactoPrincipal;

    private String telefono;

    @Builder.Default
    private Boolean estaActiva = true;

    private LocalDateTime createdAt;
}
