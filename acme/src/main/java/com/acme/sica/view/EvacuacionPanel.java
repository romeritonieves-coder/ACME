package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.EvacuacionController;
import com.acme.sica.dto.EvacuacionPersonaDto;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EvacuacionPanel extends DesktopPanel {
    private final EvacuacionController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Visita", "Documento", "Persona", "Tipo", "Empresa destino", "Punto entrada",
                    "Fecha entrada", "Placa"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField filter = new JTextField(25);
    private final JLabel summary = new JLabel(" ");
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sica-evacuation-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private List<EvacuacionPersonaDto> peopleInside = List.of();
    private ScheduledFuture<?> refreshTask;
    private volatile boolean closed;

    public EvacuacionPanel(SicaApplicationContext context) {
        controller = new EvacuacionController(context);
        buildUi();
    }

    private void buildUi() {
        add(DesktopTheme.title("Reporte de evacuación · personas actualmente dentro"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        filterRow.setOpaque(false);
        filterRow.add(new JLabel("Filtrar persona, documento, empresa o tipo:"), BorderLayout.WEST);
        filterRow.add(filter, BorderLayout.CENTER);
        center.add(filterRow, BorderLayout.NORTH);
        DesktopTheme.styleTable(table);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        summary.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        center.add(summary, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                renderFiltered();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                renderFiltered();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                renderFiltered();
            }
        });
    }

    @Override
    void onActivate() {
        refresh();
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleAtFixedRate(this::refresh, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    void onDeactivate() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    private void refresh() {
        if (closed || !refreshing.compareAndSet(false, true)) {
            return;
        }
        UiTasks.run(this, "No fue posible obtener el reporte de evacuación.",
                controller::listarDentro, rows -> {
                    peopleInside = rows;
                    renderFiltered();
                    refreshing.set(false);
                }, message -> {
                    summary.setText(message);
                    summary.setForeground(DesktopTheme.RED);
                    refreshing.set(false);
                });
    }

    private void renderFiltered() {
        String query = filter.getText().trim().toLowerCase();
        List<EvacuacionPersonaDto> filtered = peopleInside.stream().filter(person ->
                query.isBlank() || contains(person.getPersonaNombre(), query)
                        || contains(person.getDocumentoIdentidad(), query)
                        || contains(person.getEmpresaDestino(), query)
                        || contains(person.getTipoPersona(), query)).toList();
        tableModel.setRowCount(0);
        filtered.forEach(person -> tableModel.addRow(new Object[]{person.getVisitaId(),
                person.getDocumentoIdentidad(), person.getPersonaNombre(), person.getTipoPersona(),
                person.getEmpresaDestino(), person.getPuntoEntrada(), person.getFechaEntrada(),
                person.getVehiculoPlaca()}));
        summary.setText(filtered.size() + " persona(s) coinciden · " + peopleInside.size()
                + " dentro · actualización automática cada 10 s");
        summary.setForeground(DesktopTheme.MUTED);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    @Override
    public void close() {
        closed = true;
        onDeactivate();
        scheduler.shutdownNow();
    }
}
