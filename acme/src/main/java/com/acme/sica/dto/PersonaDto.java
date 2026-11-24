package com.acme.sica.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaDto {
    private Integer id;
    private String nombre;
    private String documentoIdentidad;
    private String tipoPersona;
    private Integer empresaId;
    private String empresaNombre;
    private Integer estadoAccesoId;
    private String estadoAccesoNombre;
    private String motivoRestriccion;
    private String telefono;
    private String urlFoto;
    private boolean activo;
}
