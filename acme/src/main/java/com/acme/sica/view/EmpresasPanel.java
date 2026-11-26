package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.EmpresasController;
import com.acme.sica.model.Empresa;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public final class EmpresasPanel extends DesktopPanel {
    private final EmpresasController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "NIT", "Ubicación", "Contacto", "Teléfono", "Activa"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField name = new JTextField(18);
    private final JTextField nit = new JTextField(14);
    private final JTextField location = new JTextField(16);
    private final JTextField contact = new JTextField(16);
    private final JTextField phone = new JTextField(12);
    private final JLabel status = new JLabel(" ");
    private Integer selectedId;

    public EmpresasPanel(SicaApplicationContext context) {
        controller = new EmpresasController(context);
        buildUi();
    }

    private void buildUi() {
        add(DesktopTheme.title("Empresas"), BorderLayout.NORTH);
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
        form.setBorder(BorderFactory.createTitledBorder("Ficha de empresa"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        field(form, c, 0, "Nombre", name);
        field(form, c, 1, "NIT", nit);
        field(form, c, 2, "Ubicación", location);
        field(form, c, 3, "Contacto principal", contact);
        field(form, c, 4, "Teléfono", phone);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton save = new JButton("Guardar");
        save.setEnabled(SessionManager.getInstance().hasPermission("USUARIOS_CREAR")
                || SessionManager.getInstance().hasPermission("USUARIOS_EDITAR"));
        save.addActionListener(event -> save());
        JButton delete = new JButton("Eliminar");
        delete.setEnabled(SessionManager.getInstance().hasPermission("USUARIOS_ELIMINAR"));
        delete.addActionListener(event -> delete());
        JButton clear = new JButton("Limpiar");
        clear.addActionListener(event -> clearForm());
        buttons.add(save);
        buttons.add(delete);
        buttons.add(clear);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 5;
        form.add(buttons, c);
        c.gridy++;
        form.add(status, c);
        center.add(form, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private void field(JPanel panel, GridBagConstraints c, int x, String label, java.awt.Component component) {
        c.gridx = x;
        c.gridy = 0;
        panel.add(new JLabel(label), c);
        c.gridy = 1;
        panel.add(component, c);
    }

    private void reload() {
        UiTasks.run(this, "No fue posible consultar empresas.", controller::listar, this::showCompanies,
                this::showError);
    }

    private void showCompanies(List<Empresa> companies) {
        tableModel.setRowCount(0);
        companies.forEach(company -> tableModel.addRow(new Object[]{company.getId(), company.getNombre(),
                company.getNit(), company.getUbicacion(), company.getContactoPrincipal(), company.getTelefono(),
                Boolean.TRUE.equals(company.getEstaActiva()) ? "Sí" : "No"}));
        status.setText(companies.size() + " empresa(s)");
        status.setForeground(DesktopTheme.MUTED);
    }

    private void selectRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int row = table.convertRowIndexToModel(selectedRow);
        selectedId = (Integer) tableModel.getValueAt(row, 0);
        name.setText(value(row, 1));
        nit.setText(value(row, 2));
        location.setText(value(row, 3));
        contact.setText(value(row, 4));
        phone.setText(value(row, 5));
    }

    private String value(int row, int column) {
        Object item = tableModel.getValueAt(row, column);
        return item == null ? "" : item.toString();
    }

    private void save() {
        Empresa company = Empresa.builder().id(selectedId).nombre(name.getText().trim())
                .nit(nit.getText().trim()).ubicacion(location.getText().trim())
                .contactoPrincipal(contact.getText().trim()).telefono(phone.getText().trim())
                .estaActiva(true).build();
        UiTasks.run(this, "No fue posible guardar la empresa.", () -> controller.guardar(company), saved -> {
            status.setText("Empresa guardada: " + saved.getNombre());
            status.setForeground(DesktopTheme.GREEN);
            reload();
        }, this::showError);
    }

    private void delete() {
        if (selectedId == null) {
            showError("Seleccione una empresa.");
            return;
        }
        UiTasks.run(this, "No fue posible eliminar la empresa.",
                () -> {
                    controller.eliminar(selectedId);
                    return null;
                }, result -> {
                    clearForm();
                    reload();
                }, this::showError);
    }

    private void clearForm() {
        selectedId = null;
        table.clearSelection();
        name.setText("");
        nit.setText("");
        location.setText("");
        contact.setText("");
        phone.setText("");
    }

    private void showError(String message) {
        status.setText(message);
        status.setForeground(DesktopTheme.RED);
    }

    @Override
    void onActivate() {
        reload();
    }
}
