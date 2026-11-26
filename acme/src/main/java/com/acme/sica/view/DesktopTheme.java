package com.acme.sica.view;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DesktopTheme {
    static final Color BACKGROUND = new Color(38, 40, 41);
    static final Color PANEL = new Color(45, 48, 49);
    static final Color MUTED = new Color(168, 174, 171);
    static final Color GREEN = new Color(112, 145, 119);
    static final Color AMBER = new Color(181, 145, 81);
    static final Color RED = new Color(163, 101, 100);
    private static final Logger LOGGER = Logger.getLogger(DesktopTheme.class.getName());

    private DesktopTheme() {
    }

    static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return label;
    }

    static JPanel logoBadge(int width, int height) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        badge.setBackground(new Color(235, 237, 234));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 214, 209)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        badge.setPreferredSize(new Dimension(width, height));
        try {
            BufferedImage image = ImageIO.read(DesktopTheme.class.getResource("/img/logo-acme.png"));
            double scale = Math.min((double) (width - 28) / image.getWidth(),
                    (double) (height - 24) / image.getHeight());
            int imageWidth = (int) Math.round(image.getWidth() * scale);
            int imageHeight = (int) Math.round(image.getHeight() * scale);
            Image scaled = image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
            badge.add(new JLabel(new ImageIcon(scaled)));
        } catch (IOException | IllegalArgumentException failure) {
            LOGGER.log(Level.WARNING, "Could not load the SICA logo resource.", failure);
            JLabel fallback = new JLabel("ACME  |  SICA");
            fallback.setForeground(new Color(34, 39, 43));
            fallback.setFont(fallback.getFont().deriveFont(java.awt.Font.BOLD, 20f));
            badge.add(fallback);
        }
        badge.setOpaque(true);
        return badge;
    }

    static void styleTable(javax.swing.JTable table) {
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(10, 34));
        table.setBorder(BorderFactory.createEmptyBorder());
    }

    static void setButtonColor(javax.swing.JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(new Color(245, 246, 244));
        button.setMargin(new Insets(8, 14, 8, 14));
    }
}
