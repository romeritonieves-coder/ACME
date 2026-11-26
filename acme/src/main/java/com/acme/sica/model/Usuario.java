package com.acme.sica.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
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
