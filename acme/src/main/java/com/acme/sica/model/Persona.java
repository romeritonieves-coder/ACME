package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "documento_identidad", nullable = false, unique = true, length = 20)
    private String documentoIdentidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(name = "tipo_persona", nullable = false, length = 20)
    private String tipoPersona; // Trabajador, Invitado

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_acceso_id", nullable = false)
    private PersonaEstadoAcceso estadoAcceso;

    @Column(name = "motivo_restriccion", columnDefinition = "TEXT")
    private String motivoRestriccion;

    @Column(name = "telefono", length = 25)
    private String telefono;

    @Column(name = "url_foto", length = 255)
    private String urlFoto;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
