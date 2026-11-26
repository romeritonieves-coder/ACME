package com.acme.sica.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SvgBackgroundPanel extends JPanel implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SvgBackgroundPanel.class.getName());
    private static final FlatSVGIcon SVG_ICON = loadIcon();
    private static final Object SVG_RENDER_LOCK = new Object();
    private static final ThreadFactory RENDER_THREAD_FACTORY = task -> {
        Thread thread = new Thread(task, "sica-svg-background");
        thread.setDaemon(true);
        return thread;
    };

    private final ExecutorService renderer = Executors.newSingleThreadExecutor(RENDER_THREAD_FACTORY);
    private final AtomicReference<Dimension> pendingSize = new AtomicReference<>();
    private final AtomicBoolean renderScheduled = new AtomicBoolean();
    private volatile BufferedImage cachedImage;
    private volatile int cachedWidth;
    private volatile int cachedHeight;
    private volatile Dimension lastRequestedSize;
    private volatile boolean closed;

    public SvgBackgroundPanel() {
        this(new java.awt.BorderLayout());
    }

    public SvgBackgroundPanel(java.awt.LayoutManager layout) {
        super(layout);
        setOpaque(false);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                requestRasterize(getWidth(), getHeight());
            }

            @Override
            public void componentShown(ComponentEvent event) {
                requestRasterize(getWidth(), getHeight());
            }
        });
    }

    private static FlatSVGIcon loadIcon() {
        URL resource = SvgBackgroundPanel.class.getResource("/img/sica-security-background.svg");
        if (resource == null) {
            LOGGER.warning("SICA SVG background resource is missing; using the dark gradient fallback.");
            return null;
        }
        try {
            FlatSVGIcon icon = new FlatSVGIcon(resource);
            if (!icon.hasFound() || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                LOGGER.warning("SICA SVG background could not be loaded; using the dark gradient fallback.");
                return null;
            }
            return icon;
        } catch (RuntimeException failure) {
            LOGGER.log(Level.WARNING, "Could not load SICA SVG background; using the dark gradient fallback.",
                    failure);
            return null;
        }
    }

    private void requestRasterize(int width, int height) {
        if (closed || width <= 0 || height <= 0) {
            return;
        }
        Dimension requested = new Dimension(width, height);
        if (requested.equals(lastRequestedSize)
                || (cachedWidth == width && cachedHeight == height && pendingSize.get() == null)) {
            return;
        }
        lastRequestedSize = requested;
        pendingSize.set(requested);
        if (renderScheduled.compareAndSet(false, true)) {
            renderer.execute(this::renderPendingSizes);
        }
    }

    private void renderPendingSizes() {
        try {
            Dimension size;
            while (!closed && (size = pendingSize.getAndSet(null)) != null) {
                BufferedImage image = rasterize(size.width, size.height);
                if (image == null) {
                    continue;
                }
                Dimension renderedSize = size;
                SwingUtilities.invokeLater(() -> {
                    if (!closed && renderedSize.equals(lastRequestedSize)
                            && renderedSize.width == getWidth() && renderedSize.height == getHeight()) {
                        cachedImage = image;
                        cachedWidth = renderedSize.width;
                        cachedHeight = renderedSize.height;
                        repaint();
                    }
                });
            }
        } finally {
            renderScheduled.set(false);
            if (!closed && pendingSize.get() != null
                    && renderScheduled.compareAndSet(false, true)) {
                renderer.execute(this::renderPendingSizes);
            }
        }
    }

    private BufferedImage rasterize(int width, int height) {
        if (SVG_ICON == null) {
            return null;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.max((double) width / SVG_ICON.getIconWidth(),
                    (double) height / SVG_ICON.getIconHeight());
            double offsetX = (width - SVG_ICON.getIconWidth() * scale) / 2.0;
            double offsetY = (height - SVG_ICON.getIconHeight() * scale) / 2.0;
            synchronized (SVG_RENDER_LOCK) {
                graphics.translate(offsetX, offsetY);
                graphics.scale(scale, scale);
                SVG_ICON.paintIcon(this, graphics, 0, 0);
            }
            return image;
        } catch (RuntimeException failure) {
            LOGGER.log(Level.WARNING, "Could not rasterize SICA SVG background; using the dark gradient fallback.",
                    failure);
            return null;
        } finally {
            graphics.dispose();
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        BufferedImage image = cachedImage;
        if (image != null && cachedWidth == getWidth() && cachedHeight == getHeight()) {
            graphics.drawImage(image, 0, 0, null);
            return;
        }
        Graphics2D fallback = (Graphics2D) graphics.create();
        try {
            fallback.setPaint(new GradientPaint(0, 0, DesktopTheme.BACKGROUND,
                    getWidth(), getHeight(), DesktopTheme.BACKGROUND_SECONDARY));
            fallback.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            fallback.dispose();
        }
        requestRasterize(getWidth(), getHeight());
    }

    @Override
    public void close() {
        closed = true;
        pendingSize.set(null);
        renderer.shutdownNow();
        cachedImage = null;
    }
}
