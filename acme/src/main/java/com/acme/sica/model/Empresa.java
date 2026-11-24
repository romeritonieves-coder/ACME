package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "nit", unique = true, length = 30)
    private String nit;

    @Column(name = "ubicacion", length = 100)
    private String ubicacion;

    @Column(name = "contacto_principal", length = 100)
    private String contactoPrincipal;

    @Column(name = "telefono", length = 25)
    private String telefono;

    @Column(name = "esta_activa", nullable = false)
    @Builder.Default
    private Boolean estaActiva = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
