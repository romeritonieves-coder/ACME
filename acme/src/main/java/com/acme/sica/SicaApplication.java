package com.acme.sica;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.view.DesktopTheme;
import com.acme.sica.view.LoginFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SicaApplication {
    private static final Logger LOGGER = Logger.getLogger(SicaApplication.class.getName());

    private SicaApplication() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        configureTheme();
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame(new SicaApplicationContext()).setVisible(true);
            } catch (RuntimeException failure) {
                LOGGER.log(Level.SEVERE, "Could not start SICA Desktop.", failure);
                JOptionPane.showMessageDialog(null,
                        "No fue posible iniciar SICA. Verifique la conexión y la configuración de la base de datos.\n"
                                + failure.getMessage(),
                        "Error de inicio", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void configureTheme() {
        UIManager.put("Panel.background", DesktopTheme.BACKGROUND);
        UIManager.put("Viewport.background", DesktopTheme.BACKGROUND);
        UIManager.put("TabbedPane.background", DesktopTheme.PANEL);
        UIManager.put("Table.background", DesktopTheme.PANEL);
        UIManager.put("Table.alternateRowColor", DesktopTheme.PANEL_HOVER);
        UIManager.put("Table.foreground", DesktopTheme.TEXT);
        UIManager.put("Table.selectionBackground", DesktopTheme.PRIMARY.darker());
        UIManager.put("Table.selectionForeground", DesktopTheme.TEXT);
        UIManager.put("Component.focusColor", DesktopTheme.PRIMARY);
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 8);
        UIManager.put("defaultFont", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        UIManager.put("Label.foreground", DesktopTheme.TEXT);
        UIManager.put("TextField.background", DesktopTheme.PANEL);
        UIManager.put("TextField.foreground", DesktopTheme.TEXT);
        UIManager.put("TextField.caretForeground", DesktopTheme.TEXT);
        UIManager.put("PasswordField.background", DesktopTheme.PANEL);
        UIManager.put("PasswordField.foreground", DesktopTheme.TEXT);
        UIManager.put("PasswordField.caretForeground", DesktopTheme.TEXT);
    }
}
