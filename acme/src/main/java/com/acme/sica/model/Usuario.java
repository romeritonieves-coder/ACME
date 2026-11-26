package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    private Integer id;

    private String nombre;

    private String email;

    private String password;

    private Rol rol;

    private Empresa empresa;

    @Builder.Default
    private Boolean estaActivo = true;

    @Builder.Default
    private Integer intentosFallidos = 0;

    private LocalDateTime ultimoAcceso;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
