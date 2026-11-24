package com.acme.sica.pattern.observer;

/**
 * PATRÓN OBSERVER:
 * Interfaz que deben implementar todos los observadores que reaccionan a eventos de acceso.
 */
public interface AccesoObserver {
    void onEventoAcceso(AccesoEvento evento);
}
