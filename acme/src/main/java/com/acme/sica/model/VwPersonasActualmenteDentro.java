package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "vw_personas_actualmente_dentro")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VwPersonasActualmenteDentro {

    @Id
    @Column(name = "visita_id")
    private Integer visitaId;

    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    @Column(name = "persona_nombre")
    private String personaNombre;

    @Column(name = "tipo_persona")
    private String tipoPersona;

    @Column(name = "empresa_destino")
    private String empresaDestino;

    @Column(name = "punto_entrada")
    private String puntoEntrada;

    @Column(name = "fecha_entrada")
    private LocalDateTime fechaEntrada;

    @Column(name = "vehiculo_placa")
    private String vehiculoPlaca;
}
