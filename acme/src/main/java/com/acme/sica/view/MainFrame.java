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
import java.awt.Color;
import java.awt.Dimension;
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
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JLabel workspaceTitle = DesktopTheme.title("Sistema Integrado de Control de Acceso");
        workspaceTitle.setFont(workspaceTitle.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        header.add(workspaceTitle, BorderLayout.WEST);
        JPanel sessionInfo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        sessionInfo.setOpaque(false);
        com.acme.sica.model.Usuario user = SessionManager.getInstance().getCurrentUser();
        JLabel name = new JLabel(user == null ? "Usuario" : user.getNombre());
        name.setFont(name.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        JLabel role = new JLabel(SessionManager.getInstance().getCurrentRole());
        role.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 12");
        role.setOpaque(true);
        role.setBackground(new Color(62, 69, 64));
        role.setForeground(new Color(210, 222, 211));
        role.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        connectionLabel.setForeground(DesktopTheme.MUTED);
        connectionLabel.setFont(connectionLabel.getFont().deriveFont(12f));
        sessionInfo.add(name);
        sessionInfo.add(role);
        sessionInfo.add(connectionLabel);
        header.add(sessionInfo, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        content.add(cards, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(32, 34, 35));
        sidebar.setPreferredSize(new Dimension(252, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 17, 20, 17));
        JPanel top = new JPanel(new BorderLayout(0, 26));
        top.setOpaque(false);
        JPanel logoHeader = new JPanel(new BorderLayout());
        logoHeader.setOpaque(false);
        logoHeader.add(DesktopTheme.logoBadge(218, 66), BorderLayout.CENTER);
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
        JButton logout = new JButton("↪  Cerrar sesión");
        logout.setHorizontalAlignment(JButton.LEFT);
        logout.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 8));
        logout.addActionListener(event -> logout());
        sidebar.add(logout, BorderLayout.SOUTH);
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
            button.setBackground(selected ? new Color(55, 66, 58) : new Color(32, 34, 35));
            button.setForeground(selected ? new Color(222, 232, 224) : new Color(200, 204, 201));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, selected ? 3 : 0, 0, 0,
                            selected ? DesktopTheme.GREEN : new Color(32, 34, 35)),
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

    private void logout() {
        loggingOut = true;
        SessionManager.getInstance().endSession();
        dispose();
    }

    private void stopPanels() {
        panels.values().stream().filter(DesktopPanel.class::isInstance)
                .map(DesktopPanel.class::cast).forEach(DesktopPanel::close);
    }
}
