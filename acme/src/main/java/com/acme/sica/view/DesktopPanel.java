package com.acme.sica.view;

import javax.swing.JPanel;
import java.awt.BorderLayout;

abstract class DesktopPanel extends JPanel implements AutoCloseable {
    DesktopPanel() {
        super(new BorderLayout(12, 12));
        setOpaque(false);
    }

    void onActivate() {
    }

    void onDeactivate() {
    }

    @Override
    public void close() {
        onDeactivate();
    }
}
