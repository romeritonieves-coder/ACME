package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.LoginController;
import com.acme.sica.dto.LoginResponse;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private final SicaApplicationContext context;
    private final JTextField email = new JTextField(25);
    private final JPasswordField password = new JPasswordField(25);
    private final JButton loginButton = new JButton("Iniciar sesión");
    private final JLabel feedback = new JLabel(" ");
    private boolean handedOff;

    public LoginFrame(SicaApplicationContext context) {
        super("SICA | Control de Acceso");
        this.context = context;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(560, 500));
        setResizable(false);
        setLocationRelativeTo(null);
        buildUi();
        loginButton.addActionListener(event -> authenticate());
        password.addActionListener(event -> authenticate());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (!handedOff) {
                    context.close();
                }
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(30, 48, 36, 48));
        root.add(DesktopTheme.logoBadge(340, 112), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(26, 22, 8, 22));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(7, 0, 7, 0);
        JLabel heading = DesktopTheme.title("Acceso seguro");
        heading.setHorizontalAlignment(JLabel.CENTER);
        form.add(heading, c);
        c.gridy++;
        form.add(new JLabel("Correo electrónico"), c);
        c.gridy++;
        form.add(email, c);
        c.gridy++;
        form.add(new JLabel("Contraseña"), c);
        c.gridy++;
        form.add(password, c);
        c.gridy++;
        DesktopTheme.setButtonColor(loginButton, DesktopTheme.GREEN);
        form.add(loginButton, c);
        c.gridy++;
        feedback.setForeground(DesktopTheme.AMBER);
        feedback.setHorizontalAlignment(JLabel.CENTER);
        form.add(feedback, c);
        root.add(form, BorderLayout.CENTER);

        JLabel footer = new JLabel("Sistema Integrado de Control de Acceso");
        footer.setForeground(DesktopTheme.MUTED);
        footer.setHorizontalAlignment(JLabel.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        SwingUtilities.invokeLater(email::requestFocusInWindow);
    }

    private void authenticate() {
        char[] secret = password.getPassword();
        String address = email.getText().trim();
        if (address.isBlank() || secret.length == 0) {
            Arrays.fill(secret, '\0');
            feedback.setText("Ingrese correo y contraseña.");
            feedback.setForeground(DesktopTheme.AMBER);
            return;
        }
        loginButton.setEnabled(false);
        loginButton.setText("Validando…");
        feedback.setText("Conectando con SICA...");
        feedback.setForeground(DesktopTheme.MUTED);
        UiTasks.run(this, "No fue posible iniciar sesión.", () -> {
            try {
                return new LoginController(context).autenticar(address, secret);
            } finally {
                Arrays.fill(secret, '\0');
            }
        }, this::finishLogin, message -> {
            feedback.setText(message);
            feedback.setForeground(DesktopTheme.RED);
            loginButton.setEnabled(true);
            loginButton.setText("Iniciar sesión");
            password.setText("");
        });
    }

    private void finishLogin(LoginResponse response) {
        if (!response.isAutenticado()) {
            feedback.setText(response.getMensaje());
            feedback.setForeground(DesktopTheme.RED);
            password.setText("");
            loginButton.setEnabled(true);
            loginButton.setText("Iniciar sesión");
            return;
        }
        handedOff = true;
        new MainFrame(context).setVisible(true);
        dispose();
    }
}
