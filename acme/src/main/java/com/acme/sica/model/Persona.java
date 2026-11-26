package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {

    private Integer id;

    private String nombre;

    private String documentoIdentidad;

    private Empresa empresa;

    private String tipoPersona; // Trabajador, Invitado

    private PersonaEstadoAcceso estadoAcceso;

    private String motivoRestriccion;

    private String telefono;

    private String urlFoto;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
