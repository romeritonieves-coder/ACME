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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
    private final JLabel userLabel = new JLabel();
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
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        header.add(DesktopTheme.title("SICA"), BorderLayout.WEST);
        userLabel.setForeground(DesktopTheme.MUTED);
        userLabel.setText(SessionManager.getInstance().getCurrentUser().getNombre()
                + "  ·  " + SessionManager.getInstance().getCurrentRole());
        header.add(userLabel, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        content.add(cards, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(32, 34, 35));
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 20, 16));
        JPanel top = new JPanel(new BorderLayout(0, 22));
        top.setOpaque(false);
        top.add(DesktopTheme.logoBadge(210, 74), BorderLayout.NORTH);
        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new javax.swing.BoxLayout(menu, javax.swing.BoxLayout.Y_AXIS));
        for (Module module : new MainController().visibleModules()) {
            modules.put(module.id(), module);
            JButton button = new JButton(module.icon() + "   " + module.label());
            button.setHorizontalAlignment(JButton.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            button.setBackground(sidebar.getBackground());
            button.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 8));
            button.addActionListener(event -> showModule(module.id()));
            menu.add(button);
            menu.add(javax.swing.Box.createVerticalStrut(5));
        }
        top.add(menu, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.NORTH);
        JButton logout = new JButton("↪  Cerrar sesión");
        logout.setHorizontalAlignment(JButton.LEFT);
        logout.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 8));
        logout.addActionListener(event -> logout());
        sidebar.add(logout, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showModule(String id) {
        if (!modules.containsKey(id)) {
            return;
        }
        if (activePanel instanceof DesktopPanel lifecycle) {
            lifecycle.onDeactivate();
        }
        cardLayout.show(cards, id);
        activePanel = panels.get(id);
        if (activePanel instanceof DesktopPanel lifecycle) {
            lifecycle.onActivate();
        }
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
