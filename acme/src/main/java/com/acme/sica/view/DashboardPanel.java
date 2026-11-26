package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.DashboardController;
import com.acme.sica.dto.DashboardSnapshot;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DashboardPanel extends DesktopPanel {
    private final DashboardController controller;
    private final Consumer<String> navigate;
    private final Consumer<Boolean> connectionStatus;
    private final JPanel kpiCards = new JPanel(new GridLayout(1, 0, 14, 0));
    private final JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JLabel loadStatus = new JLabel("Cargando resumen…");
    private final DefaultTableModel visitsModel = new DefaultTableModel(
            new Object[]{"Persona", "Empresa", "Estado", "Hora de entrada"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable visitsTable = new JTable(visitsModel);
    private final JPanel alertsList = new JPanel();
    private final AtomicBoolean loading = new AtomicBoolean();
    private final Timer refreshTimer;
    private final Map<String, KpiCard> cards = new LinkedHashMap<>();
    private boolean closed;

    public DashboardPanel(SicaApplicationContext context, Consumer<String> navigate,
                         Consumer<Boolean> connectionStatus) {
        controller = new DashboardController(context);
        this.navigate = navigate;
        this.connectionStatus = connectionStatus;
        buildUi();
        refreshTimer = new Timer(30_000, event -> refresh());
        refreshTimer.setCoalesce(true);
    }

    private void buildUi() {
        setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = DesktopTheme.title("Panel de control");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        JLabel subtitle = new JLabel("Resumen operativo de SICA");
        subtitle.setForeground(DesktopTheme.MUTED);
        titles.add(title);
        titles.add(subtitle);
        heading.add(titles, BorderLayout.WEST);
        loadStatus.setForeground(DesktopTheme.MUTED);
        heading.add(loadStatus, BorderLayout.EAST);
        top.add(heading, BorderLayout.NORTH);

        kpiCards.setOpaque(false);
        addKpiIfAllowed("peopleInside", "Personas dentro", "EVACUACION_VER", DesktopTheme.GREEN);
        addKpiIfAllowed("pendingRequests", "Solicitudes pendientes", "ACCESO_APROBAR", DesktopTheme.AMBER);
        addKpiIfAllowed("openIncidents", "Incidentes abiertos", "INCIDENTES_VER", DesktopTheme.RED);
        if (hasAny("ACCESO_CHECKIN", "ACCESO_CHECKOUT", "ACCESO_SOLICITAR", "ACCESO_APROBAR")) {
            addKpi("visitsToday", "Visitas de hoy", "Actividad de hoy", DesktopTheme.GREEN);
        }
        top.add(kpiCards, BorderLayout.CENTER);

        JPanel quickSection = new JPanel(new BorderLayout(0, 8));
        quickSection.setOpaque(false);
        JLabel quickHeading = new JLabel("Accesos rápidos");
        quickHeading.setFont(quickHeading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        quickSection.add(quickHeading, BorderLayout.NORTH);
        quickActions.setOpaque(false);
        addQuickAction("Registrar ingreso", "Portería", "portería",
                hasAny("ACCESO_CHECKIN", "ACCESO_SOLICITAR", "ACCESO_PASE_TEMPORAL"));
        addQuickAction("Ver aprobaciones", "Solicitudes pendientes", "aprobaciones",
                SessionManager.getInstance().hasPermission("ACCESO_APROBAR"));
        addQuickAction("Reportar incidente", "Nuevo incidente", "incidentes",
                SessionManager.getInstance().hasPermission("INCIDENTES_REGISTRAR"));
        addQuickAction("Censo de evacuación", "Personas dentro", "evacuación",
                SessionManager.getInstance().hasPermission("EVACUACION_VER"));
        quickSection.add(quickActions, BorderLayout.CENTER);
        top.add(quickSection, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        JPanel visitsCard = panelCard("Últimos movimientos", new BorderLayout(0, 8));
        DesktopTheme.styleTable(visitsTable);
        visitsTable.setRowHeight(27);
        visitsTable.getColumnModel().getColumn(2).setCellRenderer(new StateBadgeRenderer());
        visitsCard.add(new JScrollPane(visitsTable), BorderLayout.CENTER);
        lower.add(visitsCard);

        JPanel activityCard = panelCard("Alertas y actividad", new BorderLayout(0, 8));
        alertsList.setOpaque(false);
        alertsList.setLayout(new javax.swing.BoxLayout(alertsList, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane activityScroll = new JScrollPane(alertsList);
        activityScroll.setBorder(BorderFactory.createEmptyBorder());
        activityScroll.getViewport().setOpaque(false);
        activityScroll.setOpaque(false);
        activityCard.add(activityScroll, BorderLayout.CENTER);
        lower.add(activityCard);
        add(lower, BorderLayout.CENTER);
    }

    private void addKpiIfAllowed(String key, String title, String permission, Color accent) {
        if (SessionManager.getInstance().hasPermission(permission)) {
            addKpi(key, title, "Disponible según su rol", accent);
        }
    }

    private void addKpi(String key, String title, String subtitle, Color accent) {
        KpiCard card = new KpiCard(title, subtitle, accent);
        cards.put(key, card);
        kpiCards.add(card.panel);
    }

    private void addQuickAction(String text, String subtitle, String module, boolean allowed) {
        if (!allowed) {
            return;
        }
        JButton button = new JButton("<html><b>" + text + "</b><br><span style='color:#a8aeab'>"
                + subtitle + "</span></html>");
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 12");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(210, 54));
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.addActionListener(event -> navigate.accept(module));
        quickActions.add(button);
    }

    private boolean hasAny(String... permissions) {
        for (String permission : permissions) {
            if (SessionManager.getInstance().hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private JPanel panelCard(String title, java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 16");
        panel.setBackground(DesktopTheme.PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(62, 66, 66)),
                BorderFactory.createEmptyBorder(14, 15, 14, 15)));
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private void refresh() {
        if (closed || !loading.compareAndSet(false, true)) {
            return;
        }
        loadStatus.setText("Actualizando…");
        UiTasks.run(this, "No fue posible cargar el dashboard.", controller::load, this::showSnapshot,
                message -> {
                    if (!closed) {
                        loadStatus.setText("No disponible · " + message);
                        loadStatus.setForeground(DesktopTheme.RED);
                        connectionStatus.accept(false);
                    }
                    loading.set(false);
                });
    }

    private void showSnapshot(DashboardSnapshot snapshot) {
        if (closed) {
            loading.set(false);
            return;
        }
        setValue("peopleInside", snapshot.peopleInside());
        setValue("pendingRequests", snapshot.pendingRequests());
        setValue("openIncidents", snapshot.openIncidents());
        setValue("visitsToday", snapshot.visitsToday());
        visitsModel.setRowCount(0);
        snapshot.recentVisits().forEach(visit -> visitsModel.addRow(new Object[]{
                visit.person(), visit.company(), visit.state(), visit.entryTime()
        }));
        showActivity(snapshot.recentActivity());
        loadStatus.setText("Actualizado ahora");
        loadStatus.setForeground(DesktopTheme.MUTED);
        connectionStatus.accept(true);
        loading.set(false);
    }

    private void setValue(String key, Long value) {
        KpiCard card = cards.get(key);
        if (card == null) {
            return;
        }
        card.setValue(value == null ? "—" : value.toString(), value != null && value > 0);
    }

    private void showActivity(List<DashboardSnapshot.ActivityItem> items) {
        alertsList.removeAll();
        if (items.isEmpty()) {
            JLabel empty = new JLabel("No hay alertas recientes.");
            empty.setForeground(DesktopTheme.MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            alertsList.add(empty);
        } else {
            items.forEach(item -> {
                JPanel row = new JPanel(new BorderLayout(8, 3));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(65, 68, 68)),
                        BorderFactory.createEmptyBorder(7, 3, 8, 3)));
                JLabel heading = new JLabel(item.title() == null ? "Actividad" : item.title());
                heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 12.5f));
                JLabel timestamp = new JLabel(item.timestamp());
                timestamp.setForeground(DesktopTheme.MUTED);
                JPanel titleRow = new JPanel(new BorderLayout());
                titleRow.setOpaque(false);
                titleRow.add(heading, BorderLayout.CENTER);
                titleRow.add(timestamp, BorderLayout.EAST);
                JLabel detail = new JLabel("<html>" + escape(item.detail()) + "</html>");
                detail.setForeground(DesktopTheme.MUTED);
                row.add(titleRow, BorderLayout.NORTH);
                row.add(detail, BorderLayout.CENTER);
                alertsList.add(row);
            });
        }
        alertsList.revalidate();
        alertsList.repaint();
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    void onActivate() {
        refresh();
        refreshTimer.start();
    }

    @Override
    void onDeactivate() {
        refreshTimer.stop();
    }

    @Override
    public void close() {
        closed = true;
        refreshTimer.stop();
    }

    private static final class KpiCard {
        private final JPanel panel = new JPanel(new BorderLayout(0, 7));
        private final JLabel value = new JLabel("—");
        private final JLabel subtitle = new JLabel();
        private final Color accent;

        private KpiCard(String title, String description, Color accent) {
            this.accent = accent;
            panel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 16");
            panel.setBackground(new Color(48, 51, 52));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(64, 68, 68)),
                    BorderFactory.createEmptyBorder(13, 15, 12, 15)));
            JLabel heading = new JLabel(title);
            heading.setForeground(DesktopTheme.MUTED);
            heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 13f));
            value.setFont(value.getFont().deriveFont(java.awt.Font.BOLD, 30f));
            value.setForeground(new Color(232, 234, 232));
            subtitle.setText(description);
            subtitle.setForeground(DesktopTheme.MUTED);
            panel.add(heading, BorderLayout.NORTH);
            panel.add(value, BorderLayout.CENTER);
            panel.add(subtitle, BorderLayout.SOUTH);
            panel.setMinimumSize(new Dimension(160, 108));
        }

        private void setValue(String text, boolean alert) {
            value.setText(text);
            value.setForeground(alert ? accent : new Color(232, 234, 232));
        }
    }

    private static final class StateBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            String state = value == null ? "" : value.toString();
            Color color = state.equalsIgnoreCase("Dentro") || state.equalsIgnoreCase("Aprobado")
                    ? DesktopTheme.GREEN
                    : state.toLowerCase().contains("pendiente") ? DesktopTheme.AMBER
                    : state.toLowerCase().contains("rechaz") || state.toLowerCase().contains("bloque")
                    ? DesktopTheme.RED : DesktopTheme.MUTED;
            label.setForeground(selected ? table.getSelectionForeground() : color);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return label;
        }
    }
}
