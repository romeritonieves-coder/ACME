package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "visita_id", nullable = false)
    private Visita visita;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destinatario_usuario_id", nullable = false)
    private Usuario destinatarioUsuario;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo; // SOLICITUD_INGRESO, PASE_TEMPORAL, RESPUESTA_APROBACION, ALERTA_SEGURIDAD

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
}
