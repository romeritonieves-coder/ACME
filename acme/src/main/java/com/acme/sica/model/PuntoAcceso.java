package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "puntos_acceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuntoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo; // Peatonal, Vehicular, Mixto

    @Column(name = "esta_activo", nullable = false)
    @Builder.Default
    private Boolean estaActivo = true;
}
