package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "persona_estados_acceso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaEstadoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_estado", nullable = false, unique = true, length = 60)
    private String nombreEstado;
}
