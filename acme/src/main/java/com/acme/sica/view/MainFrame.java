package com.acme.sica.view;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.controller.MainController;
import com.acme.sica.controller.MainController.Module;
import com.acme.sica.security.SessionManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainFrame extends JFrame {
    private final SicaApplicationContext context;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Map<String, JPanel> panels = new LinkedHashMap<>();
    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final JLabel connectionLabel = new JLabel("●  Verificando BD");
    private JPanel activePanel;
    private boolean loggingOut;

    public MainFrame(SicaApplicationContext context) {
        super("SICA | Sistema Integrado de Control de Acceso");
        this.context = context;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 740));
        setSize(1360, 860);
        setLocationRelativeTo(null);
        buildPanels();
        buildNavigation();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                stopPanels();
                if (loggingOut) {
                    new LoginFrame(context).setVisible(true);
                } else {
                    context.close();
                }
            }
        });
        if (!modules.isEmpty()) {
            showModule(modules.keySet().iterator().next());
        }
    }

    private void buildPanels() {
        addPanel("inicio", new DashboardPanel(context, this::showModule, this::updateConnectionStatus));
        addPanel("portería", new PorteriaPanel(context));
        addPanel("aprobaciones", new AprobacionesPanel(context));
        addPanel("personas", new PersonasPanel(context));
        addPanel("empresas", new EmpresasPanel(context));
        addPanel("incidentes", new IncidentesPanel(context));
        addPanel("evacuación", new EvacuacionPanel(context));
        addPanel("auditoría", new AuditoriaPanel(context));
    }

    private void addPanel(String id, JPanel panel) {
        panels.put(id, panel);
        cards.add(panel, id);
    }

    private void buildNavigation() {
        JPanel root = new CommandCenterPanel();
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
        JLabel workspaceTitle = DesktopTheme.title("SICA");
        workspaceTitle.setFont(workspaceTitle.getFont().deriveFont(java.awt.Font.BOLD, 23f));
        workspaceTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        JLabel tagline = new JLabel("Control centralizado de accesos, visitantes y seguridad corporativa.");
        tagline.setForeground(DesktopTheme.MUTED);
        tagline.setFont(tagline.getFont().deriveFont(12f));
        titleBlock.add(workspaceTitle);
        titleBlock.add(tagline);
        header.add(titleBlock, BorderLayout.WEST);
        JPanel sessionInfo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        sessionInfo.setOpaque(false);
        com.acme.sica.model.Usuario user = SessionManager.getInstance().getCurrentUser();
        JLabel avatar = new AvatarLabel(initials(user == null ? null : user.getNombre()));
        JLabel name = new JLabel(user == null ? "Usuario" : user.getNombre());
        name.setFont(name.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        JLabel role = new JLabel(SessionManager.getInstance().getCurrentRole() == null
                ? "Sin rol" : SessionManager.getInstance().getCurrentRole());
        role.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 12");
        role.setOpaque(true);
        role.setBackground(DesktopTheme.PANEL);
        role.setForeground(DesktopTheme.INFO);
        role.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        connectionLabel.setForeground(DesktopTheme.MUTED);
        connectionLabel.setFont(connectionLabel.getFont().deriveFont(12f));
        JLabel active = new JLabel("●  Sistema activo");
        active.setForeground(DesktopTheme.GREEN);
        active.setFont(active.getFont().deriveFont(12f));
        sessionInfo.add(avatar);
        sessionInfo.add(name);
        sessionInfo.add(role);
        sessionInfo.add(active);
        sessionInfo.add(connectionLabel);
        header.add(sessionInfo, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        cards.setOpaque(false);
        content.add(cards, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(DesktopTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(252, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, DesktopTheme.BORDER),
                BorderFactory.createEmptyBorder(24, 17, 20, 17)));
        JPanel top = new JPanel(new BorderLayout(0, 26));
        top.setOpaque(false);
        JPanel logoHeader = new JPanel(new BorderLayout(0, 10));
        logoHeader.setOpaque(false);
        logoHeader.add(DesktopTheme.logoBadge(218, 66), BorderLayout.NORTH);
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new javax.swing.BoxLayout(brandText, javax.swing.BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("SICA");
        brand.setForeground(DesktopTheme.TEXT);
        brand.setFont(brand.getFont().deriveFont(java.awt.Font.BOLD, 17f));
        JLabel description = new JLabel("<html>Sistema Integrado de<br>Control de Acceso</html>");
        description.setForeground(DesktopTheme.MUTED);
        description.setFont(description.getFont().deriveFont(11f));
        brandText.add(brand);
        brandText.add(javax.swing.Box.createVerticalStrut(3));
        brandText.add(description);
        logoHeader.add(brandText, BorderLayout.CENTER);
        top.add(logoHeader, BorderLayout.NORTH);
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new javax.swing.BoxLayout(menu, javax.swing.BoxLayout.Y_AXIS));
        for (Module module : new MainController().visibleModules()) {
            modules.put(module.id(), module);
            JButton button = new JButton(module.icon() + "   " + module.label());
            button.setHorizontalAlignment(JButton.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            button.setBackground(sidebar.getBackground());
            button.setForeground(DesktopTheme.MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(1, 0, 1, 0),
                    BorderFactory.createEmptyBorder(10, 14, 10, 8)));
            button.setFocusPainted(false);
            button.addActionListener(event -> showModule(module.id()));
            navigationButtons.put(module.id(), button);
            menu.add(button);
            menu.add(javax.swing.Box.createVerticalStrut(5));
        }
        top.add(menu, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel sidebarFooter = new JPanel(new BorderLayout(0, 10));
        sidebarFooter.setOpaque(false);
        JButton logout = new JButton("↪  Cerrar sesión");
        logout.setHorizontalAlignment(JButton.LEFT);
        logout.setForeground(new java.awt.Color(255, 171, 171));
        logout.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 8));
        logout.addActionListener(event -> logout());
        JLabel version = new JLabel("Sistema operativo · v1.0");
        version.setForeground(DesktopTheme.MUTED);
        version.setFont(version.getFont().deriveFont(10f));
        version.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        sidebarFooter.add(logout, BorderLayout.NORTH);
        sidebarFooter.add(version, BorderLayout.SOUTH);
        sidebar.add(sidebarFooter, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showModule(String id) {
        if (!modules.containsKey(id) || !panels.containsKey(id)) {
            return;
        }
        if (activePanel instanceof DesktopPanel lifecycle) {
            lifecycle.onDeactivate();
        }
        cardLayout.show(cards, id);
        activePanel = panels.get(id);
        navigationButtons.forEach((moduleId, button) -> {
            boolean selected = moduleId.equals(id);
            button.setBackground(selected ? new java.awt.Color(29, 52, 82) : DesktopTheme.SIDEBAR);
            button.setForeground(selected ? DesktopTheme.TEXT : DesktopTheme.MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0,
                            selected ? DesktopTheme.PRIMARY : DesktopTheme.SIDEBAR),
                    BorderFactory.createEmptyBorder(10, selected ? 11 : 14, 10, 8)));
        });
        if (activePanel instanceof DesktopPanel lifecycle) {
            lifecycle.onActivate();
        }
    }

    private void updateConnectionStatus(boolean connected) {
        connectionLabel.setText(connected ? "●  Base de datos conectada" : "●  Sin conexión a BD");
        connectionLabel.setForeground(connected ? DesktopTheme.GREEN : DesktopTheme.RED);
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(java.util.Locale.ROOT);
    }

    private void logout() {
        loggingOut = true;
        SessionManager.getInstance().endSession();
        dispose();
    }

    private void stopPanels() {
        panels.values().stream().filter(DesktopPanel.class::isInstance)
                .map(DesktopPanel.class::cast).forEach(DesktopPanel::close);
    }

    private static final class CommandCenterPanel extends JPanel {
        private CommandCenterPanel() {
            super(new BorderLayout());
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setPaint(new GradientPaint(0, 0, DesktopTheme.BACKGROUND,
                        getWidth(), getHeight(), DesktopTheme.BACKGROUND_SECONDARY));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new java.awt.Color(47, 128, 237, 9));
                for (int x = getWidth() - 300; x < getWidth(); x += 22) {
                    g.drawLine(x, 0, x - 210, Math.min(290, getHeight()));
                }
                g.setColor(new java.awt.Color(56, 189, 248, 7));
                for (int x = 0; x < Math.min(350, getWidth()); x += 24) {
                    g.drawLine(x, getHeight(), x + 210, Math.max(0, getHeight() - 250));
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static final class AvatarLabel extends JLabel {
        private AvatarLabel(String initials) {
            super(initials, JLabel.CENTER);
            setForeground(DesktopTheme.TEXT);
            setFont(getFont().deriveFont(java.awt.Font.BOLD, 12f));
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new java.awt.Color(35, 71, 117));
                g.fillOval(1, 1, getWidth() - 3, getHeight() - 3);
                g.setColor(DesktopTheme.PRIMARY);
                g.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
