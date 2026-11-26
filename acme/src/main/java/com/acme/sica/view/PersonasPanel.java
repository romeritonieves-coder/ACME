package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.PersonasController;
import com.acme.sica.dto.PersonaDto;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

public final class PersonasPanel extends DesktopPanel {
    private final PersonasController controller;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Documento", "Tipo", "Empresa ID", "Empresa",
                    "Estado", "Teléfono", "URL foto"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField filter = new JTextField(22);
    private final JTextField name = new JTextField(18);
    private final JTextField document = new JTextField(14);
    private final JComboBox<String> type = new JComboBox<>(new String[]{"Invitado", "Trabajador"});
    private final JTextField company = new JTextField(8);
    private final JTextField phone = new JTextField(12);
    private final JTextField photoUrl = new JTextField(20);
    private final JLabel status = new JLabel(" ");
    private PersonaDto selected;

    public PersonasPanel(SicaApplicationContext context) {
        controller = new PersonasController(context);
        buildUi();
    }

    private void buildUi() {
        add(DesktopTheme.title("Personas"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(filter, BorderLayout.CENTER);
        JButton search = new JButton("Buscar");
        search.addActionListener(event -> reload());
        searchRow.add(search, BorderLayout.EAST);
        center.add(searchRow, BorderLayout.NORTH);
        DesktopTheme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectRow();
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(buildForm(), BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createTitledBorder("Ficha de persona"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 5, 3, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addField(form, c, 0, 0, "Nombre", name);
        addField(form, c, 1, 0, "Documento", document);
        addField(form, c, 2, 0, "Tipo", type);
        addField(form, c, 0, 2, "ID empresa (opcional)", company);
        addField(form, c, 1, 2, "Teléfono", phone);
        addField(form, c, 2, 2, "URL foto", photoUrl);
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton save = new JButton("Guardar");
        save.setEnabled(SessionManager.getInstance().hasPermission("USUARIOS_CREAR")
                || SessionManager.getInstance().hasPermission("USUARIOS_EDITAR"));
        save.addActionListener(event -> save());
        JButton delete = new JButton("Eliminar");
        delete.setEnabled(SessionManager.getInstance().hasPermission("USUARIOS_ELIMINAR"));
        delete.addActionListener(event -> delete());
        JButton clear = new JButton("Limpiar");
        clear.addActionListener(event -> clearForm());
        actions.add(save);
        actions.add(delete);
        actions.add(clear);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        form.add(actions, c);
        c.gridy++;
        form.add(status, c);
        return form;
    }

    private void addField(JPanel panel, GridBagConstraints c, int x, int y, String label,
                          java.awt.Component input) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        panel.add(new JLabel(label), c);
        c.gridy++;
        panel.add(input, c);
    }

    private void reload() {
        UiTasks.run(this, "No fue posible consultar personas.", () -> controller.buscar(filter.getText().trim()),
                this::showPeople, this::showError);
    }

    private void showPeople(List<PersonaDto> people) {
        tableModel.setRowCount(0);
        people.forEach(person -> tableModel.addRow(new Object[]{person.getId(), person.getNombre(),
                person.getDocumentoIdentidad(), person.getTipoPersona(), person.getEmpresaId(),
                person.getEmpresaNombre(), person.getEstadoAccesoNombre(), person.getTelefono(),
                person.getUrlFoto()}));
        status.setText(people.size() + " persona(s)");
        status.setForeground(DesktopTheme.MUTED);
    }

    private void selectRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int row = table.convertRowIndexToModel(selectedRow);
        Object companyValue = tableModel.getValueAt(row, 4);
        selected = PersonaDto.builder().id((Integer) tableModel.getValueAt(row, 0))
                .nombre(stringValue(row, 1)).documentoIdentidad(stringValue(row, 2))
                .tipoPersona(stringValue(row, 3))
                .empresaId(companyValue instanceof Integer id ? id : null)
                .empresaNombre(stringValue(row, 5)).estadoAccesoNombre(stringValue(row, 6))
                .telefono(stringValue(row, 7)).urlFoto(stringValue(row, 8)).build();
        name.setText(selected.getNombre());
        document.setText(selected.getDocumentoIdentidad());
        type.setSelectedItem(selected.getTipoPersona());
        company.setText(selected.getEmpresaId() == null ? "" : selected.getEmpresaId().toString());
        phone.setText(selected.getTelefono() == null ? "" : selected.getTelefono());
        photoUrl.setText(selected.getUrlFoto() == null ? "" : selected.getUrlFoto());
    }

    private String stringValue(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void save() {
        Integer companyId = parseCompany();
        if (companyId == Integer.MIN_VALUE) {
            return;
        }
        PersonaDto person = PersonaDto.builder().id(selected == null ? null : selected.getId())
                .nombre(name.getText().trim()).documentoIdentidad(document.getText().trim())
                .tipoPersona((String) type.getSelectedItem()).empresaId(companyId)
                .telefono(phone.getText().trim()).urlFoto(photoUrl.getText().trim()).build();
        UiTasks.run(this, "No fue posible guardar a la persona.", () -> controller.guardar(person), saved -> {
            selected = saved;
            status.setText("Persona guardada: " + saved.getNombre());
            status.setForeground(DesktopTheme.GREEN);
            reload();
        }, this::showError);
    }

    private Integer parseCompany() {
        if (company.getText().isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(company.getText().trim());
        } catch (NumberFormatException failure) {
            showError("El ID de empresa debe ser numérico.");
            return Integer.MIN_VALUE;
        }
    }

    private void delete() {
        if (selected == null) {
            showError("Seleccione una persona para eliminar.");
            return;
        }
        UiTasks.run(this, "No fue posible eliminar a la persona.",
                () -> {
                    controller.eliminar(selected.getId());
                    return null;
                }, result -> {
                    showError("Persona eliminada.");
                    clearForm();
                    reload();
                }, this::showError);
    }

    private void clearForm() {
        selected = null;
        name.setText("");
        document.setText("");
        type.setSelectedIndex(0);
        company.setText("");
        phone.setText("");
        photoUrl.setText("");
        table.clearSelection();
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
