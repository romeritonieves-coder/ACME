package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    private Long id;

    private Visita visita;

    private Usuario destinatarioUsuario;

    private String tipo; // SOLICITUD_INGRESO, PASE_TEMPORAL, RESPUESTA_APROBACION, ALERTA_SEGURIDAD

    private String mensaje;

    @Builder.Default
    private Boolean leida = false;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaLectura;
}
