package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.logging.Logger;

/**
 * PATRÓN STRATEGY (Context):
 * Resuelve y ejecuta dinámicamente la estrategia de acceso correspondiente
 * al flujo solicitado según los 4 flujos obligatorios del SICA.
 */
@RequiredArgsConstructor
public class AccesoStrategyContext {
    private static final Logger LOGGER = Logger.getLogger(AccesoStrategyContext.class.getName());

    private final List<AccesoStrategy> strategies;

    public AccesoResponseDto ejecutarEstrategia(TipoFlujoAcceso tipoFlujo,
                                               AccesoRequestDto request,
                                               Persona persona,
                                               Usuario usuarioActual,
                                               PuntoAcceso puntoAcceso) {

        AccesoStrategy estrategia = strategies.stream()
                .filter(s -> s.aplicaPara(tipoFlujo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró estrategia para el flujo: " + tipoFlujo));

        LOGGER.fine(() -> "Executing " + estrategia.getClass().getSimpleName()
                + " for person ID " + persona.getId());

        return estrategia.procesar(request, persona, usuarioActual, puntoAcceso);
    }
}
