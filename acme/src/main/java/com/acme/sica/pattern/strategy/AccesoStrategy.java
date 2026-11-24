package com.acme.sica.pattern.strategy;

import com.acme.sica.dto.AccesoRequestDto;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.model.Persona;
import com.acme.sica.model.PuntoAcceso;
import com.acme.sica.model.Usuario;

/**
 * PATRÓN STRATEGY:
 * Interfaz que encapsula la lógica algorítmica específica para cada uno de los flujos de acceso
 * dentro del Complejo Acme.
 */
public interface AccesoStrategy {

    boolean aplicaPara(TipoFlujoAcceso tipoFlujo);

    AccesoResponseDto procesar(AccesoRequestDto request,
                               Persona persona,
                               Usuario usuarioActual,
                               PuntoAcceso puntoAcceso);
}
