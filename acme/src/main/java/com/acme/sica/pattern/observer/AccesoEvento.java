package com.acme.sica.pattern.observer;

import com.acme.sica.model.Persona;
import com.acme.sica.model.Visita;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccesoEvento {
    private String tipo; // SOLICITUD_INGRESO, PASE_TEMPORAL, RESPUESTA_APROBACION, ALERTA_SEGURIDAD, CHECK_IN, CHECK_OUT
    private Integer visitaId;
    private Visita visita;
    private Persona persona;
    private Integer destinatarioUsuarioId;
    private String mensaje;
    private String estadoVisita;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}
