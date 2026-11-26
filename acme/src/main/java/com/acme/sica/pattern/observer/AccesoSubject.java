package com.acme.sica.pattern.observer;

import com.acme.sica.config.DatabaseConnectionManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AccesoSubject implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AccesoSubject.class.getName());
    private static final ThreadFactory DAEMON_THREADS = task -> {
        Thread thread = new Thread(task, "sica-access-observer");
        thread.setDaemon(true);
        return thread;
    };

    private final List<AccesoObserver> observers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, DAEMON_THREADS);
    private final DatabaseConnectionManager database;

    public AccesoSubject() {
        this(DatabaseConnectionManager.getInstance());
    }

    public AccesoSubject(DatabaseConnectionManager database) {
        this.database = database;
    }

    public void registrarObservador(AccesoObserver observer) {
        observers.add(observer);
    }

    public void removerObservador(AccesoObserver observer) {
        observers.remove(observer);
    }

    public void notificarObservadores(AccesoEvento evento) {
        database.afterCommit(() -> executor.execute(() -> observers.forEach(observer -> {
            try {
                observer.onEventoAcceso(evento);
            } catch (RuntimeException failure) {
                LOGGER.log(Level.SEVERE, "Error al notificar observador " + observer.getClass().getName(), failure);
            }
        })));
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
