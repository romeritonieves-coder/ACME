package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.PorteriaController;
import com.acme.sica.dto.AccesoResponseDto;
import com.acme.sica.dto.PersonaDto;
import com.acme.sica.dto.TipoFlujoAcceso;
import com.acme.sica.pattern.observer.AccesoEvento;
import com.acme.sica.pattern.observer.AccesoObserver;
import com.acme.sica.security.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PorteriaPanel extends DesktopPanel implements AccesoObserver {
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private final SicaApplicationContext context;
    private final PorteriaController controller;
    private final JTextField document = new JTextField(22);
    private final JTextField companyId = new JTextField(10);
    private final JTextField hostId = new JTextField(10);
    private final JTextField reason = new JTextField(30);
    private final JLabel personDetails = new JLabel("Busque una persona por documento para comenzar.");
    private final JLabel photo = new JLabel("Sin foto", JLabel.CENTER);
    private final JLabel status = new JLabel(" ");
    private final JTextArea eventLog = new JTextArea(4, 42);
    private final AtomicBoolean closed = new AtomicBoolean();
    private PersonaDto selectedPerson;

    public PorteriaPanel(SicaApplicationContext context) {
        this.context = context;
        controller = new PorteriaController(context);
        buildUi();
        context.accesoSubject().registrarObservador(this);
    }

    private void buildUi() {
        add(DesktopTheme.title("Portería"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(18, 12));
        center.setOpaque(false);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(5, 0, 7, 8);
        form.add(new JLabel("Documento de identidad"), c);
        c.gridy++;
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(document, BorderLayout.CENTER);
        JButton search = new JButton("Buscar");
        search.addActionListener(event -> searchPerson());
        searchRow.add(search, BorderLayout.EAST);
        form.add(searchRow, c);
        c.gridy++;
        c.insets = new Insets(8, 0, 8, 8);
        personDetails.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.PANEL),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        form.add(personDetails, c);
        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(4, 0, 4, 8);
        form.add(new JLabel("ID empresa destino"), c);
        c.gridx = 1;
        form.add(new JLabel("ID funcionario anfitrión"), c);
        c.gridy++;
        c.gridx = 0;
        form.add(companyId, c);
        c.gridx = 1;
        form.add(hostId, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        form.add(new JLabel("Motivo de visita"), c);
        c.gridy++;
        form.add(reason, c);
        c.gridy++;
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        addAction(actions, "Check-in trabajador", TipoFlujoAcceso.TRABAJADOR_REGULAR,
                "ACCESO_CHECKIN", 0, 0);
        addAction(actions, "Invitado pre-registrado", TipoFlujoAcceso.INVITADO_PRE_REGISTRADO,
                "ACCESO_CHECKIN", 1, 0);
        addAction(actions, "Invitado no anunciado", TipoFlujoAcceso.INVITADO_NO_ANUNCIADO,
                "ACCESO_SOLICITAR", 0, 1);
        addAction(actions, "Pase temporal", TipoFlujoAcceso.CARNET_OLVIDADO,
                "ACCESO_PASE_TEMPORAL", 1, 1);
        JButton checkOut = new JButton("Registrar check-out");
        checkOut.setEnabled(SessionManager.getInstance().hasPermission("ACCESO_CHECKOUT"));
        checkOut.addActionListener(event -> runAccess("CHECK_OUT", controller::registrarSalida));
        actions.add(checkOut, gridCell(0, 2));
        JButton regularize = new JButton("Regularizar salida olvidada");
        regularize.setEnabled(SessionManager.getInstance().hasPermission("ACCESO_REGULARIZAR"));
        regularize.addActionListener(event -> runAccess("SALIDA_OLVIDADA", controller::regularizarSalida));
        actions.add(regularize, gridCell(1, 2));
        form.add(actions, c);
        c.gridy++;
        c.insets = new Insets(12, 0, 4, 8);
        status.setForeground(DesktopTheme.MUTED);
        form.add(status, c);
        center.add(form, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(8, 12));
        right.setOpaque(false);
        photo.setPreferredSize(new Dimension(270, 230));
        photo.setBorder(BorderFactory.createLineBorder(DesktopTheme.PANEL));
        right.add(photo, BorderLayout.NORTH);
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        eventLog.setBackground(DesktopTheme.PANEL);
        eventLog.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        right.add(new JScrollPane(eventLog), BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
    }

    private void addAction(JPanel actions, String label, TipoFlujoAcceso flow, String permission,
                           int x, int y) {
        JButton button = new JButton(label);
        button.setEnabled(SessionManager.getInstance().hasPermission(permission));
        button.addActionListener(event -> runFlow(flow));
        actions.add(button, gridCell(x, y));
    }

    private GridBagConstraints gridCell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        return c;
    }

    private void searchPerson() {
        String identity = document.getText().trim();
        if (identity.isBlank()) {
            showStatus("Ingrese el documento que desea buscar.", DesktopTheme.AMBER);
            return;
        }
        showStatus("Buscando persona...", DesktopTheme.MUTED);
        UiTasks.run(this, "No fue posible buscar a la persona.", () -> controller.buscarPersona(identity),
                result -> {
                    if (result.isEmpty()) {
                        selectedPerson = null;
                        personDetails.setText("No se encontró una persona con ese documento.");
                        photo.setIcon(null);
                        photo.setText("Sin foto");
                        showStatus("Registre la persona desde el módulo Personas antes de continuar.", DesktopTheme.AMBER);
                        return;
                    }
                    selectedPerson = result.get();
                    personDetails.setText("<html><b>" + escape(selectedPerson.getNombre()) + "</b><br>"
                            + escape(selectedPerson.getTipoPersona()) + " · "
                            + escape(selectedPerson.getEstadoAccesoNombre()) + "<br>"
                            + escape(selectedPerson.getEmpresaNombre()) + "</html>");
                    loadPhoto(selectedPerson.getUrlFoto());
                    showStatus("Persona lista para la operación.", DesktopTheme.GREEN);
                }, message -> showStatus(message, DesktopTheme.RED));
    }

    private void loadPhoto(String address) {
        photo.setIcon(null);
        photo.setText("Cargando foto…");
        new javax.swing.SwingWorker<Image, Void>() {
            @Override
            protected Image doInBackground() throws Exception {
                if (address == null || address.isBlank()) {
                    return null;
                }
                URL url = new URL(address);
                if (!"https".equalsIgnoreCase(url.getProtocol()) && !"http".equalsIgnoreCase(url.getProtocol())) {
                    throw new IOException("La URL de foto debe usar HTTP o HTTPS.");
                }
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                String contentType = connection.getContentType();
                if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
                    throw new IOException("La URL no devolvió una imagen.");
                }
                try (InputStream input = connection.getInputStream()) {
                    byte[] bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
                    if (bytes.length > MAX_IMAGE_BYTES) {
                        throw new IOException("La imagen supera el límite de 8 MB.");
                    }
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null) {
                        throw new IOException("No fue posible interpretar la imagen.");
                    }
                    return image.getScaledInstance(260, 220, Image.SCALE_SMOOTH);
                }
            }

            @Override
            protected void done() {
                try {
                    Image image = get();
                    if (image == null) {
                        photo.setText("Sin foto registrada");
                    } else {
                        photo.setText("");
                        photo.setIcon(new ImageIcon(image));
                    }
                } catch (Exception failure) {
                    photo.setText("Foto no disponible");
                    showStatus(UiTasks.messageOf(failure), DesktopTheme.AMBER);
                }
            }
        }.execute();
    }

    private void runFlow(TipoFlujoAcceso flow) {
        if (selectedPerson == null) {
            showStatus("Busque primero una persona válida.", DesktopTheme.AMBER);
            return;
        }
        Integer company = parseOptionalInteger(companyId.getText(), "ID de empresa");
        Integer host = parseOptionalInteger(hostId.getText(), "ID de anfitrión");
        if (company == INVALID_ID || host == INVALID_ID) {
            return;
        }
        String identity = selectedPerson.getDocumentoIdentidad();
        UiTasks.run(this, "No fue posible registrar el flujo de acceso.",
                () -> controller.procesar(identity, flow, company, host, reason.getText().trim()),
                this::showResponse, message -> showStatus(message, DesktopTheme.RED));
    }

    private void runAccess(String label, java.util.function.Function<String, AccesoResponseDto> operation) {
        String identity = selectedPerson == null ? document.getText().trim() : selectedPerson.getDocumentoIdentidad();
        if (identity.isBlank()) {
            showStatus("Busque una persona o ingrese su documento.", DesktopTheme.AMBER);
            return;
        }
        UiTasks.run(this, "No fue posible completar " + label + ".",
                () -> operation.apply(identity), this::showResponse,
                message -> showStatus(message, DesktopTheme.RED));
    }

    private void showResponse(AccesoResponseDto response) {
        Color color = response.isExito() ? DesktopTheme.GREEN : DesktopTheme.RED;
        showStatus(response.getMensaje() + (response.getCodigoPase() == null
                ? "" : "  Código: " + response.getCodigoPase()), color);
        if (response.isExito() && response.isRequiereAprobacion()) {
            eventLog.append("Solicitud #" + response.getVisitaId() + " enviada para aprobación.\n");
        }
    }

    private Integer parseOptionalInteger(String value, String field) {
        if (value == null || value.isBlank()) {
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

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void onEventoAcceso(AccesoEvento evento) {
        if (closed.get()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!closed.get()) {
                eventLog.append(evento.getFecha() + " · " + evento.getTipo() + " · " + evento.getMensaje() + "\n");
                eventLog.setCaretPosition(eventLog.getDocument().getLength());
                if (evento.getTipo() != null && evento.getTipo().contains("APROBACION")) {
                    showStatus(evento.getMensaje(), DesktopTheme.GREEN);
                }
            }
        });
    }

    @Override
    public void close() {
        closed.set(true);
        context.accesoSubject().removerObservador(this);
    }
}
