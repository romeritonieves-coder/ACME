package com.acme.sica.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    private Integer id;

    private String placa;

    private String tipo; // Automovil, Motocicleta, Camioneta, Carga, Otro

    private String marcaModelo;

    private String color;

    private Persona persona;
}
