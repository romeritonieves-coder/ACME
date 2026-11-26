package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.AprobacionesController;
import com.acme.sica.controller.DashboardController;
import com.acme.sica.dto.DashboardSnapshot;
import com.acme.sica.model.VwSolicitudesPendientes;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoObserver;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AprobacionesPanel extends DesktopPanel implements AccesoObserver {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AprobacionesController controller;
    private final DashboardController dashboardController;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Visita", "Visitante", "Documento", "Tipo", "Estado", "Empresa", "Anfitrión", "Fecha"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JPanel requestCards = new JPanel(new CardLayout());
    private final JPanel emptyState = new JPanel(new BorderLayout(8, 8));
    private final JPanel kpiCards = new JPanel(new GridLayout(1, 0, 12, 0));
    private final JTextField search = new JTextField(22);
    private final JTextField dateFilter = new JTextField(10);
    private final JLabel status = new JLabel(" ");
    private final JLabel refreshed = new JLabel("Actualización automática activa");
    private final JButton approveButton = new JButton("✓  Aprobar");
    private final JButton rejectButton = new JButton("×  Rechazar");
    private final JButton refreshButton = new JButton("↻  Actualizar");
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final AtomicBoolean loadingMetrics = new AtomicBoolean();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sica-approval-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final Timer searchTimer;
    private final List<ApprovalKpi> kpis = new ArrayList<>();
    private List<VwSolicitudesPendientes> allRequests = List.of();
    private ScheduledFuture<?> refreshTask;
    private ScheduledFuture<?> metricsTask;
    private volatile boolean closed;

    public AprobacionesPanel(SicaApplicationContext context) {
        controller = new AprobacionesController(context);
        dashboardController = new DashboardController(context);
        searchTimer = new Timer(180, event -> applyFilters());
        searchTimer.setRepeats(false);
        controller.suscribir(this);
        buildUi();
    }

    private void buildUi() {
        JPanel pageHeader = new JPanel(new BorderLayout(0, 4));
        pageHeader.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = DesktopTheme.title("Aprobaciones en tiempo real");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        JLabel description = new JLabel("Revisa y gestiona las solicitudes de acceso pendientes.");
        description.setForeground(DesktopTheme.MUTED);
        heading.add(title);
        heading.add(description);
        pageHeader.add(heading, BorderLayout.WEST);
        refreshed.setForeground(DesktopTheme.MUTED);
        refreshed.setHorizontalAlignment(SwingConstants.RIGHT);
        pageHeader.add(refreshed, BorderLayout.EAST);
        add(pageHeader, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setOpaque(false);
        kpiCards.setOpaque(false);
        addKpi("Solicitudes pendientes", DesktopTheme.AMBER);
        if (SessionManager.getInstance().hasPermission("ACCESO_CHECKIN")
                || SessionManager.getInstance().hasPermission("ACCESO_SOLICITAR")
                || SessionManager.getInstance().hasPermission("ACCESO_APROBAR")
                || SessionManager.getInstance().hasPermission("ACCESO_CHECKOUT")) {
            addKpi("Visitas de hoy", DesktopTheme.PRIMARY);
        }
        if (SessionManager.getInstance().hasPermission("EVACUACION_VER")) {
            addKpi("Personas dentro", DesktopTheme.GREEN);
        }
        if (SessionManager.getInstance().hasPermission("INCIDENTES_VER")) {
            addKpi("Incidentes abiertos", DesktopTheme.RED);
        }
        content.add(kpiCards, BorderLayout.NORTH);

        JPanel requestPanel = new JPanel(new BorderLayout(0, 12));
        requestPanel.setOpaque(false);
        requestPanel.add(buildFilters(), BorderLayout.NORTH);
        requestCards.setOpaque(false);
        DesktopTheme.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(38);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(76);
        table.getColumnModel().getColumn(1).setPreferredWidth(155);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(145);
        table.getColumnModel().getColumn(7).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setCellRenderer(new StateBadgeRenderer());
        table.getSelectionModel().addListSelectionListener(event -> updateActionState());
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(DesktopTheme.PANEL);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        requestCards.add(tableScroll, "table");
        buildEmptyState();
        requestCards.add(emptyState, "empty");
        requestPanel.add(requestCards, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        actions.setOpaque(false);
        DesktopTheme.setButtonColor(approveButton, DesktopTheme.GREEN);
        DesktopTheme.setButtonColor(rejectButton, DesktopTheme.RED);
        DesktopTheme.setButtonColor(refreshButton, DesktopTheme.PRIMARY);
        approveButton.setToolTipText("Aprobar la solicitud seleccionada");
        rejectButton.setToolTipText("Rechazar la solicitud seleccionada");
        refreshButton.setToolTipText("Actualizar la lista ahora");
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        approveButton.addActionListener(event -> resolve(true));
        rejectButton.addActionListener(event -> resolve(false));
        refreshButton.addActionListener(event -> refresh());
        actions.add(approveButton);
        actions.add(rejectButton);
        actions.add(refreshButton);
        footer.add(actions, BorderLayout.WEST);
        status.setForeground(DesktopTheme.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
        footer.add(status, BorderLayout.SOUTH);
        requestPanel.add(footer, BorderLayout.SOUTH);
        content.add(requestPanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private JPanel buildFilters() {
        JPanel filters = new JPanel(new BorderLayout(10, 8));
        filters.setOpaque(false);
        JLabel hint = new JLabel("Pendientes");
        hint.setForeground(DesktopTheme.INFO);
        hint.setFont(hint.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        filters.add(hint, BorderLayout.WEST);
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        fields.setOpaque(false);
        search.setToolTipText("Buscar por visitante, documento, empresa o anfitrión");
        search.putClientProperty("JTextField.placeholderText", "Buscar visitante, documento, empresa…");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }
        });
        dateFilter.setToolTipText("Filtrar por fecha, formato YYYY-MM-DD");
        dateFilter.putClientProperty("JTextField.placeholderText", "AAAA-MM-DD");
        dateFilter.setColumns(10);
        dateFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                searchTimer.restart();
            }
        });
        fields.add(search);
        fields.add(dateFilter);
        filters.add(fields, BorderLayout.EAST);
        return filters;
    }

    private void buildEmptyState() {
        emptyState.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 16");
        emptyState.setBackground(DesktopTheme.PANEL);
        emptyState.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.BORDER),
                BorderFactory.createEmptyBorder(36, 24, 36, 24)));
        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new javax.swing.BoxLayout(message, javax.swing.BoxLayout.Y_AXIS));
        JLabel icon = new JLabel("▤", SwingConstants.CENTER);
        icon.setForeground(DesktopTheme.INFO);
        icon.setFont(icon.getFont().deriveFont(java.awt.Font.PLAIN, 42f));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("No hay solicitudes pendientes");
        title.setForeground(DesktopTheme.TEXT);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Las nuevas solicitudes aparecerán aquí automáticamente.");
        subtitle.setForeground(DesktopTheme.MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.add(icon);
        message.add(javax.swing.Box.createVerticalStrut(10));
        message.add(title);
        message.add(javax.swing.Box.createVerticalStrut(6));
        message.add(subtitle);
        emptyState.add(message, BorderLayout.CENTER);
    }

    private void addKpi(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 14");
        card.setBackground(DesktopTheme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(DesktopTheme.BORDER),
                        BorderFactory.createEmptyBorder(10, 13, 10, 13))));
        JLabel label = new JLabel(title);
        label.setForeground(DesktopTheme.MUTED);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        JLabel value = new JLabel("—");
        value.setFont(value.getFont().deriveFont(java.awt.Font.BOLD, 23f));
        value.setForeground(DesktopTheme.TEXT);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        kpiCards.add(card);
        kpis.add(new ApprovalKpi(title, value, accent));
    }

    @Override
    void onActivate() {
        refresh();
        refreshMetrics();
        if (refreshTask == null || refreshTask.isCancelled()) {
            refreshTask = scheduler.scheduleAtFixedRate(
                    () -> SwingUtilities.invokeLater(this::refresh), 5, 5, TimeUnit.SECONDS);
        }
        if (!kpis.isEmpty() && (metricsTask == null || metricsTask.isCancelled())) {
            metricsTask = scheduler.scheduleAtFixedRate(
                    () -> SwingUtilities.invokeLater(this::refreshMetrics), 30, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    void onDeactivate() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
        if (metricsTask != null) {
            metricsTask.cancel(false);
            metricsTask = null;
        }
    }

    private void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        if (closed || !refreshing.compareAndSet(false, true)) {
            return;
        }
        refreshButton.setEnabled(false);
        UiTasks.run(this, "No fue posible actualizar las solicitudes.",
                controller::pendientes, this::showRequests, message -> {
                    if (!closed) {
                        status.setText(message);
                        status.setForeground(DesktopTheme.RED);
                        refreshed.setText("Error de actualización");
                        refreshed.setForeground(DesktopTheme.RED);
                    }
                    refreshButton.setEnabled(true);
                    refreshing.set(false);
                });
    }

    private void refreshMetrics() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshMetrics);
            return;
        }
        if (closed || kpis.isEmpty() || !loadingMetrics.compareAndSet(false, true)) {
            return;
        }
        UiTasks.run(this, "No fue posible cargar los indicadores.",
                dashboardController::load, snapshot -> {
                    applyMetrics(snapshot);
                    loadingMetrics.set(false);
                }, message -> {
                    if (!closed) {
                        status.setText("No se pudieron cargar algunos indicadores: " + message);
                    }
                    loadingMetrics.set(false);
                });
    }

    private void applyMetrics(DashboardSnapshot snapshot) {
        setMetric("Solicitudes pendientes", snapshot.pendingRequests());
        setMetric("Visitas de hoy", snapshot.visitsToday());
        setMetric("Personas dentro", snapshot.peopleInside());
        setMetric("Incidentes abiertos", snapshot.openIncidents());
    }

    private void setMetric(String title, Long value) {
        kpis.stream().filter(kpi -> kpi.title.equals(title)).findFirst()
                .ifPresent(kpi -> {
                    kpi.value.setText(value == null ? "—" : value.toString());
                    kpi.value.setForeground(value != null && value > 0
                            && ("Solicitudes pendientes".equals(title) || "Incidentes abiertos".equals(title))
                            ? kpi.accent : DesktopTheme.TEXT);
                });
    }

    private void showRequests(List<VwSolicitudesPendientes> requests) {
        allRequests = List.copyOf(requests);
        applyFilters();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        refreshed.setText("●  Actualizado " + timestamp + " · automático cada 5 s");
        refreshed.setForeground(DesktopTheme.GREEN);
        refreshButton.setEnabled(true);
        refreshing.set(false);
    }

    private void applyFilters() {
        if (closed) {
            return;
        }
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        String dateQuery = dateFilter.getText().trim();
        LocalDate date = null;
        if (!dateQuery.isBlank()) {
            try {
                date = LocalDate.parse(dateQuery);
                dateFilter.setBorder(javax.swing.UIManager.getBorder("TextField.border"));
            } catch (DateTimeParseException invalidDate) {
                dateFilter.setBorder(BorderFactory.createLineBorder(DesktopTheme.RED));
                status.setText("La fecha debe tener el formato AAAA-MM-DD.");
                status.setForeground(DesktopTheme.AMBER);
                return;
            }
        } else {
            dateFilter.setBorder(javax.swing.UIManager.getBorder("TextField.border"));
        }
        LocalDate finalDate = date;
        List<VwSolicitudesPendientes> filtered = allRequests.stream().filter(request ->
                matches(request.getVisitante(), query) || matches(request.getDocumentoIdentidad(), query)
                        || matches(request.getEmpresaDestino(), query) || matches(request.getAnfitrion(), query))
                .filter(request -> finalDate == null || matchesDate(request.getFechaSolicitud(), finalDate))
                .toList();
        tableModel.setRowCount(0);
        filtered.forEach(request -> tableModel.addRow(new Object[]{
                request.getVisitaId(), request.getVisitante(), request.getDocumentoIdentidad(),
                request.getTipoPersona(), request.getEstado(), request.getEmpresaDestino(),
                request.getAnfitrion(), formatDate(request.getFechaSolicitud())
        }));
        CardLayout layout = (CardLayout) requestCards.getLayout();
        layout.show(requestCards, filtered.isEmpty() ? "empty" : "table");
        status.setText(filtered.size() + " solicitud(es) pendiente(s)"
                + (filtered.size() != allRequests.size() ? " · filtradas de " + allRequests.size() : "")
                + " · actualización automática cada 5 s");
        status.setForeground(DesktopTheme.MUTED);
        updateActionState();
    }

    private boolean matches(String value, String query) {
        return query.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(query));
    }

    private boolean matchesDate(LocalDateTime timestamp, LocalDate date) {
        return timestamp != null && timestamp.toLocalDate().equals(date);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "—" : value.format(DISPLAY_DATE);
    }

    private void updateActionState() {
        boolean selected = table.getSelectedRow() >= 0;
        boolean permitted = SessionManager.getInstance().hasPermission("ACCESO_APROBAR");
        approveButton.setEnabled(selected && permitted && !refreshing.get());
        rejectButton.setEnabled(selected && permitted && !refreshing.get());
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
        if (!approved) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "¿Confirma que desea rechazar la solicitud de "
                            + tableModel.getValueAt(row, 1) + "?",
                    "Confirmar rechazo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        String note = JOptionPane.showInputDialog(this, "Observaciones (opcional):");
        if (note == null) {
            return;
        }
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        approveButton.setText(approved ? "Procesando…" : "✓  Aprobar");
        rejectButton.setText(approved ? "×  Rechazar" : "Procesando…");
        UiTasks.run(this, "No fue posible resolver la solicitud.",
                () -> controller.resolver(visitId, approved, note), response -> {
                    showToast(response.getMensaje(), approved ? DesktopTheme.GREEN : DesktopTheme.AMBER);
                    approveButton.setText("✓  Aprobar");
                    rejectButton.setText("×  Rechazar");
                    refresh();
                }, message -> {
                    showToast(message, DesktopTheme.RED);
                    approveButton.setText("✓  Aprobar");
                    rejectButton.setText("×  Rechazar");
                    updateActionState();
                });
    }

    private void showToast(String message, Color color) {
        status.setText(message);
        status.setForeground(color);
        Timer reset = new Timer(4500, event -> {
            if (!closed) {
                status.setForeground(DesktopTheme.MUTED);
            }
        });
        reset.setRepeats(false);
        reset.start();
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
                showToast(event.getMensaje(), DesktopTheme.INFO);
                refresh();
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        onDeactivate();
        searchTimer.stop();
        scheduler.shutdownNow();
        controller.cancelarSuscripcion(this);
    }

    private record ApprovalKpi(String title, JLabel value, Color accent) {
    }

    private static final class StateBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            String state = value == null ? "" : value.toString().toLowerCase(Locale.ROOT);
            Color foreground = state.contains("rechaz") ? DesktopTheme.RED
                    : state.contains("pendiente") ? DesktopTheme.AMBER
                    : state.contains("revisión") || state.contains("revision") ? DesktopTheme.INFO
                    : DesktopTheme.GREEN;
            Color background = state.contains("rechaz") ? DesktopTheme.RED_SOFT
                    : state.contains("pendiente") ? DesktopTheme.AMBER_SOFT
                    : state.contains("revisión") || state.contains("revision")
                    ? new Color(17, 51, 68) : DesktopTheme.GREEN_SOFT;
            label.setForeground(selected ? table.getSelectionForeground() : foreground);
            label.setBackground(selected ? table.getSelectionBackground() : background);
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            return label;
        }
    }
}
