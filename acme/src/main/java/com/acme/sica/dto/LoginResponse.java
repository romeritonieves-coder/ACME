package com.acme.sica.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private boolean autenticado;
    private String mensaje;
    private Integer usuarioId;
    private String nombre;
    private String email;
    private String rol;
    private Integer empresaId;
    private String empresaNombre;
    private List<String> permisos;
}
