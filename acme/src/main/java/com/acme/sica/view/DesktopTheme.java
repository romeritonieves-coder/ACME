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

public final class DesktopTheme {
    public static final Color BACKGROUND = new Color(11, 18, 32);
    public static final Color BACKGROUND_SECONDARY = new Color(15, 27, 48);
    public static final Color SIDEBAR = new Color(15, 23, 42);
    public static final Color PANEL = new Color(22, 34, 56);
    public static final Color PANEL_HOVER = new Color(29, 45, 71);
    public static final Color BORDER = new Color(43, 59, 85);
    public static final Color TEXT = new Color(248, 250, 252);
    public static final Color MUTED = new Color(168, 180, 199);
    public static final Color PRIMARY = new Color(47, 128, 237);
    public static final Color PRIMARY_HOVER = new Color(86, 161, 255);
    public static final Color GREEN = new Color(34, 197, 94);
    public static final Color GREEN_SOFT = new Color(20, 61, 42);
    public static final Color AMBER = new Color(245, 158, 11);
    public static final Color AMBER_SOFT = new Color(69, 49, 18);
    public static final Color RED = new Color(239, 68, 68);
    public static final Color RED_SOFT = new Color(69, 29, 38);
    public static final Color INFO = new Color(56, 189, 248);
    private static final Logger LOGGER = Logger.getLogger(DesktopTheme.class.getName());

    private DesktopTheme() {
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return label;
    }

    public static JPanel logoBadge(int width, int height) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        badge.setBackground(new Color(248, 250, 252));
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
            fallback.setForeground(new Color(15, 23, 42));
            fallback.setFont(fallback.getFont().deriveFont(java.awt.Font.BOLD, 20f));
            badge.add(fallback);
        }
        badge.setOpaque(true);
        return badge;
    }

    public static void styleTable(javax.swing.JTable table) {
        table.setBackground(PANEL);
        table.setForeground(TEXT);
        table.setSelectionBackground(new Color(35, 71, 117));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(10, 34));
        table.getTableHeader().setBackground(BACKGROUND_SECONDARY);
        table.getTableHeader().setForeground(MUTED);
        table.setBorder(BorderFactory.createEmptyBorder());
    }

    public static void setButtonColor(javax.swing.JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(TEXT);
        button.setMargin(new Insets(8, 14, 8, 14));
    }
}
