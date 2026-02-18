package io.github.arkosammy12.jemu.application.drivers;

import io.github.arkosammy12.jemu.backend.common.VideoGenerator;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;

public class JPanelVideoDriver extends JPanel implements VideoDriver, Closeable {

    private final VideoGenerator<?> videoGenerator;
    private final int[][] renderBuffer;

    private final int displayWidth;
    private final int displayHeight;

    private final BufferedImage bufferedImage;
    private final AffineTransform drawTransform = new AffineTransform();

    private final Thread renderThread;
    private final Object renderLock = new Object();
    protected final Object renderBufferLock = new Object();

    private volatile boolean running = true;
    private boolean frameRequested = false;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public JPanelVideoDriver(VideoGenerator<?> videoGenerator, KeyListener keyListener) {
        this.videoGenerator = videoGenerator;
        this.displayWidth = videoGenerator.getImageWidth();
        this.displayHeight = videoGenerator.getImageHeight();

        this.renderBuffer = new int[displayWidth][displayHeight];
        this.bufferedImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_ARGB);

        SwingUtilities.invokeLater(() -> this.addKeyListener(keyListener));
        this.renderThread = new Thread(this::renderLoop, "jemu-render-thread");
        this.renderThread.setDaemon(true);
        this.renderThread.start();
    }

    @Override
    public void outputFrame(int[][] argb) {
        synchronized (this.renderBufferLock) {
            if (this.renderBuffer == null || this.videoGenerator == null) {
                return;
            }
            for (int y = 0; y < this.videoGenerator.getImageHeight(); y++) {
                for (int x = 0; x < this.videoGenerator.getImageWidth(); x++) {
                    this.renderBuffer[x][y] = argb[x][y];
                }
            }
        }
    }

    public void requestFrame() {
        synchronized (this.renderLock) {
            this.frameRequested = true;
            this.renderLock.notify();
        }
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


        double scale = Math.min(w / this.displayWidth, h / this.displayHeight);

        double scaledWidth = this.displayWidth * scale;
        double scaledHeight = this.displayHeight * scale;

        double offsetX = (w - scaledWidth) / 2.0;
        double offsetY = (h - scaledHeight) / 2.0;

        this.drawTransform.setToIdentity();
        this.drawTransform.translate(offsetX, offsetY);
        this.drawTransform.scale(scale, scale);

        this.lastWidth = w;
        this.lastHeight = h;
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
