package com.acme.sica.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Table(name = "vw_solicitudes_pendientes")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VwSolicitudesPendientes {

    @Id
    @Column(name = "visita_id")
    private Integer visitaId;

    @Column(name = "visitante")
    private String visitante;

    @Column(name = "documento_identidad")
    private String documentoIdentidad;

    @Column(name = "tipo_persona")
    private String tipoPersona;

    @Column(name = "estado")
    private String estado;

    @Column(name = "empresa_destino")
    private String empresaDestino;

    @Column(name = "anfitrion")
    private String anfitrion;

    @Column(name = "es_pase_temporal")
    private Boolean esPaseTemporal;

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;
}
