package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.*;
import com.acme.sica.pattern.factory.VisitaFactory;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoSubject;
import com.acme.sica.repository.VisitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo de Trabajador Regular (ingreso directo habitual con carnet activo)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrabajadorRegularStrategy implements AccesoStrategy {

    private final VisitaRepository visitaRepository;
    private final VisitaFactory visitaFactory;
    private final AccesoSubject accesoSubject;

    @Override
    public boolean aplicaPara(TipoFlujoAcceso tipoFlujo) {
        return TipoFlujoAcceso.TRABAJADOR_REGULAR.equals(tipoFlujo);
    }

    @Override
    @Transactional
    public AccesoResponseDto procesar(AccesoRequestDto request, Persona persona, Usuario usuarioActual, PuntoAcceso puntoAcceso) {
        if (persona.getEstadoAcceso() != null && "Con Prohibicion de Ingreso".equalsIgnoreCase(persona.getEstadoAcceso().getNombreEstado())) {
            return AccesoResponseDto.builder()
                    .exito(false)
                    .mensaje("ACCESO DENEGADO: El trabajador tiene prohibición de ingreso: " + persona.getMotivoRestriccion())
                    .personaNombre(persona.getNombre())
                    .documentoIdentidad(persona.getDocumentoIdentidad())
                    .urlFoto(persona.getUrlFoto())
                    .build();
        }

        Visita visita = visitaFactory.crearVisita(
                TipoFlujoAcceso.TRABAJADOR_REGULAR,
                persona,
                null,
                usuarioActual,
                puntoAcceso,
                persona.getEmpresa(),
                null,
                request
        );

        visita = visitaRepository.save(visita);

        accesoSubject.notificarObservadores(AccesoEvento.builder()
                .tipo("CHECK_IN")
                .visitaId(visita.getId())
                .visita(visita)
                .persona(persona)
                .mensaje("Ingreso habitual de trabajador: " + persona.getNombre())
                .estadoVisita("Dentro")
                .build());

        return AccesoResponseDto.builder()
                .exito(true)
                .mensaje("Ingreso laboral registrado con éxito.")
                .visitaId(visita.getId())
                .estadoVisita("Dentro")
                .codigoPase(visita.getCodigoPase())
                .requiereAprobacion(false)
                .esPaseTemporal(false)
                .personaNombre(persona.getNombre())
                .documentoIdentidad(persona.getDocumentoIdentidad())
                .tipoPersona(persona.getTipoPersona())
                .urlFoto(persona.getUrlFoto())
                .anfitrionNombre("Jornada Laboral")
                .empresaNombre(persona.getEmpresa() != null ? persona.getEmpresa().getNombre() : "Complejo Acme")
                .build();
    }
}
