package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PATRÓN STRATEGY (Context):
 * Resuelve y ejecuta dinámicamente la estrategia de acceso correspondiente
 * al flujo solicitado según los 4 flujos obligatorios del SICA.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccesoStrategyContext {

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

        log.info("Ejecutando estrategia de acceso '{}' para persona {} (Doc: {})",
                estrategia.getClass().getSimpleName(), persona.getNombre(), persona.getDocumentoIdentidad());

        return estrategia.procesar(request, persona, usuarioActual, puntoAcceso);
    }
}
