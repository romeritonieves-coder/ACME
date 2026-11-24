package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "modulo", nullable = false, length = 50)
    private String modulo;

    @Column(name = "nombre_permiso", nullable = false, unique = true, length = 100)
    private String nombrePermiso;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
