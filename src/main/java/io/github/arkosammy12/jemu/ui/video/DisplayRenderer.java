package io.github.arkosammy12.jemu.ui.video;

import io.github.arkosammy12.jemu.systems.video.Display;
import io.github.arkosammy12.jemu.util.DisplayAngle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.util.List;

public class DisplayRenderer extends JPanel implements Closeable {

    private final Display<?> display;
    private final int[][] renderBuffer;

    private final int displayWidth;
    private final int displayHeight;
    private final DisplayAngle displayAngle;

    private final BufferedImage bufferedImage;
    private final AffineTransform rotationTransform = new AffineTransform();
    private final AffineTransform drawTransform = new AffineTransform();

    private final Thread renderThread;
    private final Object renderLock = new Object();
    protected final Object renderBufferLock = new Object();

    private volatile boolean running = true;
    private boolean frameRequested = false;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public DisplayRenderer(Display<?> display, List<KeyAdapter> keyAdapters) {
        this.display = display;
        this.displayWidth = display.getImageWidth();
        this.displayHeight = display.getImageHeight();
        this.displayAngle = display.getDisplayAngle();

        this.renderBuffer = new int[displayWidth][displayHeight];
        this.bufferedImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_ARGB);

        switch (displayAngle) {
            case DEG_90 -> {
                rotationTransform.translate(displayHeight, 0);
                rotationTransform.rotate(Math.toRadians(90));
            }
            case DEG_180 -> {
                rotationTransform.translate(displayWidth, displayHeight);
                rotationTransform.rotate(Math.toRadians(180));
            }
            case DEG_270 -> {
                rotationTransform.translate(0, displayWidth);
                rotationTransform.rotate(Math.toRadians(270));
            }
        }
        SwingUtilities.invokeLater(() -> keyAdapters.forEach(this::addKeyListener));
        this.renderThread = new Thread(this::renderLoop, "jemu-render-thread");
        this.renderThread.setDaemon(true);
        this.renderThread.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.updateTransformIfNeeded();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(this.bufferedImage, this.drawTransform, null);
        } finally {
            g2.dispose();
        }
    }

    private void updateTransformIfNeeded() {
        int w = this.getWidth();
        int h = this.getHeight();

        if (w == this.lastWidth && h == this.lastHeight) {
            return;
        }

        double logicalWidth = (this.displayAngle == DisplayAngle.DEG_90 || this.displayAngle == DisplayAngle.DEG_270) ? this.displayHeight : this.displayWidth;
        double logicalHeight = (this.displayAngle == DisplayAngle.DEG_90 || this.displayAngle == DisplayAngle.DEG_270) ? this.displayWidth : this.displayHeight;

        double scale = Math.min(w / logicalWidth, h / logicalHeight);

        double scaledWidth = logicalWidth * scale;
        double scaledHeight = logicalHeight * scale;

        double offsetX = (w - scaledWidth) / 2.0;
        double offsetY = (h - scaledHeight) / 2.0;

        this.drawTransform.setToIdentity();
        this.drawTransform.translate(offsetX, offsetY);
        this.drawTransform.scale(scale, scale);
        this.drawTransform.concatenate(rotationTransform);

        this.lastWidth = w;
        this.lastHeight = h;
    }

    public void updateRenderBuffer() {
        synchronized (this.renderBufferLock) {
            this.display.populateRenderBuffer(this.renderBuffer);
        }
    }

    public void requestFrame() {
        synchronized (this.renderLock) {
            this.frameRequested = true;
            this.renderLock.notify();
        }
    }

    private void renderLoop() {
        while (this.running) {
            synchronized (this.renderLock) {
                while (this.running && !this.frameRequested) {
                    try {
                        this.renderLock.wait();
                    } catch (InterruptedException _) {}
                }
                this.frameRequested = false;
            }
            this.renderFrame();
        }
    }

    private void renderFrame() {
        int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        synchronized (renderBufferLock) {
            for (int y = 0; y < displayHeight; y++) {
                int base = y * displayWidth;
                for (int x = 0; x < displayWidth; x++) {
                    pixels[base + x] = renderBuffer[x][y];
                }
            }
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    public void close() {
        this.running = false;
        synchronized (this.renderLock) {
            this.renderLock.notifyAll();
        }
        if (this.renderThread != null) {
            try {
                this.renderThread.join();
            } catch (InterruptedException _) {}
        }
    }

}
