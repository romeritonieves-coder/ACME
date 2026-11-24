package com.acme.sica.pattern.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PATRÓN OBSERVER (Subject):
 * Mantiene la lista de observadores registrados y publica eventos en tiempo real.
 */
@Component
@Slf4j
public class AccesoSubject {

    private final List<AccesoObserver> observers = new CopyOnWriteArrayList<>();

    public AccesoSubject(List<AccesoObserver> initialObservers) {
        if (initialObservers != null) {
            observers.addAll(initialObservers);
        }
    }

    public void registrarObservador(AccesoObserver observer) {
        observers.add(observer);
        log.info("Observador registrado: {}", observer.getClass().getSimpleName());
    }

    public void removerObservador(AccesoObserver observer) {
        observers.remove(observer);
    }

    public void notificarObservadores(AccesoEvento evento) {
        for (AccesoObserver observer : observers) {
            try {
                observer.onEventoAcceso(evento);
            } catch (Exception e) {
                log.error("Error notificando al observador {}: {}", observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
