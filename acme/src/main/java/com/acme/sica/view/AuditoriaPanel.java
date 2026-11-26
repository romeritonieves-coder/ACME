package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.AuditoriaController;
import com.acme.sica.model.BitacoraAuditoria;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

public final class AuditoriaPanel extends DesktopPanel {
    private final AuditoriaController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Fecha", "Usuario", "Correo", "Acción", "Tabla", "Registro", "Detalles"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel(" ");

    public AuditoriaPanel(SicaApplicationContext context) {
        controller = new AuditoriaController(context);
        buildUi();
    }

    private void buildUi() {
        add(DesktopTheme.title("Bitácora de auditoría"), BorderLayout.NORTH);
        DesktopTheme.styleTable(table);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JButton refresh = new JButton("Cargar últimos 100 eventos");
        refresh.addActionListener(event -> reload());
        footer.add(refresh, BorderLayout.WEST);
        footer.add(status, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
    }

    @Override
    void onActivate() {
        reload();
    }

    private void reload() {
        UiTasks.run(this, "No fue posible consultar la bitácora.", controller::listarUltimosEventos,
                this::showEvents, message -> {
                    status.setText(message);
                    status.setForeground(DesktopTheme.RED);
                });
    }

    private void showEvents(List<BitacoraAuditoria> events) {
        tableModel.setRowCount(0);
        events.forEach(event -> tableModel.addRow(new Object[]{event.getFechaHora(),
                event.getUsuario() == null ? "Sistema" : event.getUsuario().getNombre(),
                event.getUsuario() == null ? "" : event.getUsuario().getEmail(),
                event.getAccionRealizada(), event.getTablaAfectada(), event.getRegistroIdAfectado(),
                event.getDetalles()}));
        status.setText(events.size() + " evento(s) · solo lectura");
        status.setForeground(DesktopTheme.MUTED);
    }
}
