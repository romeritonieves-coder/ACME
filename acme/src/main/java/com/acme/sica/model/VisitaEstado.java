package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "visita_estados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_estado", nullable = false, unique = true, length = 80)
    private String nombreEstado;
}
