package com.acme.sica.view;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

final class UiTasks {
    private UiTasks() {
    }

    static <T> void run(Component parent, String message, Callable<T> operation,
                        Consumer<T> success, Consumer<String> failure) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operation.call();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    failure.accept("La operación fue interrumpida.");
                } catch (ExecutionException execution) {
                    failure.accept(messageOf(execution.getCause()));
                }
            }
        }.execute();
    }

    static String messageOf(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null
                && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
