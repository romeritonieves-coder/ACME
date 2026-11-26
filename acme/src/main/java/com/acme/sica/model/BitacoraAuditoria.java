package com.acme.sica.model;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraAuditoria {

    private Long id;

    private Usuario usuario;

    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    private String accionRealizada;

    private String tablaAfectada;

    private Integer registroIdAfectado;

    private String ipOrigen;

    private String detalles;
}
