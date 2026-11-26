package com.acme.sica;

import com.acme.sica.config.SicaApplicationContext;
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
        UIManager.put("Panel.background", new java.awt.Color(38, 40, 41));
        UIManager.put("Viewport.background", new java.awt.Color(38, 40, 41));
        UIManager.put("TabbedPane.background", new java.awt.Color(45, 48, 49));
        UIManager.put("Table.background", new java.awt.Color(45, 48, 49));
        UIManager.put("Table.alternateRowColor", new java.awt.Color(49, 52, 53));
        UIManager.put("Table.foreground", new java.awt.Color(226, 228, 227));
        UIManager.put("Table.selectionBackground", new java.awt.Color(82, 105, 88));
        UIManager.put("Table.selectionForeground", new java.awt.Color(247, 248, 246));
        UIManager.put("Component.focusColor", new java.awt.Color(126, 151, 132));
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 8);
        UIManager.put("defaultFont", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        UIManager.put("Label.foreground", new java.awt.Color(226, 228, 227));
        UIManager.put("TextField.background", new java.awt.Color(52, 55, 56));
        UIManager.put("TextField.foreground", new java.awt.Color(235, 236, 234));
        UIManager.put("TextField.caretForeground", new java.awt.Color(235, 236, 234));
        UIManager.put("PasswordField.background", new java.awt.Color(52, 55, 56));
        UIManager.put("PasswordField.foreground", new java.awt.Color(235, 236, 234));
        UIManager.put("PasswordField.caretForeground", new java.awt.Color(235, 236, 234));
    }
}
