package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.AprobacionesController;
import com.acme.sica.model.VwSolicitudesPendientes;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoObserver;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AprobacionesPanel extends DesktopPanel implements AccesoObserver {
    private final AprobacionesController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Visita", "Visitante", "Documento", "Tipo", "Estado", "Empresa", "Anfitrión", "Fecha"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel(" ");
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sica-approval-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> refreshTask;
    private volatile boolean closed;

    public AprobacionesPanel(SicaApplicationContext context) {
        controller = new AprobacionesController(context);
        controller.suscribir(this);
        buildUi();
    }

    private void buildUi() {
        add(DesktopTheme.title("Aprobaciones en tiempo real"), BorderLayout.NORTH);
        DesktopTheme.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton approve = new JButton("Aprobar");
        DesktopTheme.setButtonColor(approve, DesktopTheme.GREEN);
        approve.addActionListener(event -> resolve(true));
        JButton reject = new JButton("Rechazar");
        DesktopTheme.setButtonColor(reject, DesktopTheme.RED);
        reject.addActionListener(event -> resolve(false));
        JButton refresh = new JButton("Actualizar");
        refresh.addActionListener(event -> refresh());
        actions.add(approve);
        actions.add(reject);
        actions.add(refresh);
        footer.add(actions, BorderLayout.WEST);
        status.setForeground(DesktopTheme.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        footer.add(status, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
    }

    @Override
    void onActivate() {
        refresh();
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleAtFixedRate(this::refresh, 5, 5, TimeUnit.SECONDS);
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
        UiTasks.run(this, "No fue posible actualizar las solicitudes.",
                controller::pendientes, this::showRequests, message -> {
                    status.setText(message);
                    status.setForeground(DesktopTheme.RED);
                    refreshing.set(false);
                });
    }

    private void showRequests(List<VwSolicitudesPendientes> requests) {
        tableModel.setRowCount(0);
        requests.stream().forEach(request -> tableModel.addRow(new Object[]{
                request.getVisitaId(), request.getVisitante(), request.getDocumentoIdentidad(),
                request.getTipoPersona(), request.getEstado(), request.getEmpresaDestino(),
                request.getAnfitrion(), request.getFechaSolicitud()
        }));
        status.setText(requests.size() + " solicitud(es) pendiente(s) · actualización automática cada 5 s");
        status.setForeground(DesktopTheme.MUTED);
        refreshing.set(false);
    }

    private void resolve(boolean approved) {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            status.setText("Seleccione una solicitud.");
            status.setForeground(DesktopTheme.AMBER);
            return;
        }
        int row = table.convertRowIndexToModel(selected);
        Integer visitId = (Integer) tableModel.getValueAt(row, 0);
        String note = JOptionPane.showInputDialog(this, "Observaciones (opcional):");
        if (note == null) {
            return;
        }
        UiTasks.run(this, "No fue posible resolver la solicitud.",
                () -> controller.resolver(visitId, approved, note), response -> {
                    status.setText(response.getMensaje());
                    status.setForeground(approved ? DesktopTheme.GREEN : DesktopTheme.AMBER);
                    refresh();
                }, message -> {
                    status.setText(message);
                    status.setForeground(DesktopTheme.RED);
                });
    }

    @Override
    public void onEventoAcceso(AccesoEvento event) {
        if (closed) {
            return;
        }
        String type = event.getTipo();
        if (!"SOLICITUD_INGRESO".equals(type) && !"PASE_TEMPORAL".equals(type)
                && !"RESPUESTA_APROBACION".equals(type)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!closed) {
                status.setText(event.getMensaje());
                status.setForeground(DesktopTheme.AMBER);
                refresh();
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        onDeactivate();
        scheduler.shutdownNow();
        controller.cancelarSuscripcion(this);
    }
}
