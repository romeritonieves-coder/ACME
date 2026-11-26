package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.IncidentesController;
import com.acme.sica.dto.IncidenteDto;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public final class IncidentesPanel extends DesktopPanel {
    private final IncidentesController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Visita", "Fecha", "Severidad", "Estado", "Reportado por", "Descripción", "Acciones"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField visitId = new JTextField(8);
    private final JTextField pointId = new JTextField(8);
    private final JComboBox<String> severity = new JComboBox<>(new String[]{"Baja", "Media", "Alta", "Critica"});
    private final JComboBox<String> state = new JComboBox<>(new String[]{"Abierto", "En Investigacion", "Cerrado"});
    private final JTextArea description = new JTextArea(3, 26);
    private final JTextArea actionsTaken = new JTextArea(3, 26);
    private final JLabel status = new JLabel(" ");
    private Integer selectedId;

    public IncidentesPanel(SicaApplicationContext context) {
        controller = new IncidentesController(context);
        buildUi();
        if (SessionManager.getInstance().hasPermission("INCIDENTES_VER")) {
            reload();
        } else {
            showStatus("Puede registrar incidentes; el historial requiere INCIDENTES_VER.", DesktopTheme.MUTED);
        }
    }

    private void buildUi() {
        add(DesktopTheme.title("Incidentes de seguridad"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        DesktopTheme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectRow();
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createTitledBorder("Registro y seguimiento"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addField(form, c, 0, 0, "ID visita", visitId);
        addField(form, c, 1, 0, "ID punto de acceso", pointId);
        addField(form, c, 2, 0, "Severidad", severity);
        addField(form, c, 0, 2, "Estado", state);
        addField(form, c, 1, 2, "Descripción", new JScrollPane(description));
        addField(form, c, 2, 2, "Acciones tomadas", new JScrollPane(actionsTaken));
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton register = new JButton("Registrar incidente");
        register.setEnabled(SessionManager.getInstance().hasPermission("INCIDENTES_REGISTRAR"));
        register.addActionListener(event -> register());
        JButton update = new JButton("Actualizar estado");
        update.setEnabled(SessionManager.getInstance().hasPermission("INCIDENTES_GESTIONAR"));
        update.addActionListener(event -> update());
        JButton refresh = new JButton("Actualizar lista");
        refresh.addActionListener(event -> reload());
        buttons.add(register);
        buttons.add(update);
        buttons.add(refresh);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        form.add(buttons, c);
        c.gridy++;
        form.add(status, c);
        center.add(form, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void addField(JPanel panel, GridBagConstraints c, int x, int y, String label,
                          java.awt.Component component) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridy++;
        panel.add(component, c);
    }

    private void reload() {
        if (!SessionManager.getInstance().hasPermission("INCIDENTES_VER")) {
            showStatus("No tiene permiso para consultar el historial.", DesktopTheme.AMBER);
            return;
        }
        UiTasks.run(this, "No fue posible consultar incidentes.", controller::listar, incidents -> {
            tableModel.setRowCount(0);
            incidents.forEach(incident -> tableModel.addRow(new Object[]{incident.getId(), incident.getVisitaId(),
                    incident.getFecha(), incident.getSeveridad(), incident.getEstado(),
                    incident.getReportadoPorNombre(), incident.getDescripcion(), incident.getAccionesTomadas()}));
            showStatus(incidents.size() + " incidente(s)", DesktopTheme.MUTED);
        }, message -> showStatus(message, DesktopTheme.RED));
    }

    private void selectRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int row = table.convertRowIndexToModel(selectedRow);
        selectedId = (Integer) tableModel.getValueAt(row, 0);
        visitId.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        severity.setSelectedItem(tableModel.getValueAt(row, 3));
        state.setSelectedItem(tableModel.getValueAt(row, 4));
        description.setText(String.valueOf(tableModel.getValueAt(row, 6)));
        Object actions = tableModel.getValueAt(row, 7);
        actionsTaken.setText(actions == null ? "" : actions.toString());
    }

    private void register() {
        Integer visit = parse(visitId.getText(), "ID visita", false);
        Integer point = parse(pointId.getText(), "ID punto de acceso", true);
        if (visit == null || point == INVALID_ID) {
            return;
        }
        IncidenteDto dto = IncidenteDto.builder().visitaId(visit).puntoAccesoId(point)
                .severidad((String) severity.getSelectedItem()).descripcion(description.getText().trim())
                .accionesTomadas(actionsTaken.getText().trim()).build();
        UiTasks.run(this, "No fue posible registrar el incidente.", () -> controller.registrar(dto), saved -> {
            showStatus("Incidente #" + saved.getId() + " registrado.", DesktopTheme.GREEN);
            reload();
        }, message -> showStatus(message, DesktopTheme.RED));
    }

    private void update() {
        if (selectedId == null) {
            showStatus("Seleccione un incidente para actualizar.", DesktopTheme.AMBER);
            return;
        }
        UiTasks.run(this, "No fue posible actualizar el incidente.",
                () -> controller.actualizarEstado(selectedId, (String) state.getSelectedItem(),
                        actionsTaken.getText().trim()), saved -> {
                    showStatus("Estado actualizado para el incidente #" + saved.getId(), DesktopTheme.GREEN);
                    reload();
                }, message -> showStatus(message, DesktopTheme.RED));
    }

    private Integer parse(String value, String field, boolean optional) {
        if (optional && (value == null || value.isBlank())) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            showStatus(field + " debe ser numérico.", DesktopTheme.AMBER);
            return INVALID_ID;
        }
    }

    private static final Integer INVALID_ID = Integer.MIN_VALUE;

    private void showStatus(String message, java.awt.Color color) {
        status.setText(message);
        status.setForeground(color);
    }
}
