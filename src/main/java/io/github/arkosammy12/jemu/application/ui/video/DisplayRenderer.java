package io.github.arkosammy12.jemu.application.ui.video;

import io.github.arkosammy12.jemu.backend.common.SystemController;
import io.github.arkosammy12.jemu.backend.common.VideoGenerator;
import io.github.arkosammy12.jemu.backend.drivers.KeyMapping;
import io.github.arkosammy12.jemu.backend.drivers.VideoDriver;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayRenderer extends JPanel implements VideoDriver, Closeable {

    @Nullable
    private VideoGenerator<?> videoGenerator;

    @Nullable
    private InputHandler inputHandler;

    @Nullable
    private int[][] renderBuffer;

    @Nullable
    private BufferedImage bufferedImage;


    private final AffineTransform rotationTransform = new AffineTransform();
    private final AffineTransform drawTransform = new AffineTransform();

    private final Thread renderThread;
    private final Object renderLock = new Object();
    protected final Object renderBufferLock = new Object();

    private volatile boolean running = true;
    private boolean frameRequested = false;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public DisplayRenderer() {
        this.renderThread = new Thread(this::renderLoop, "jemu-render-thread");
        this.renderThread.setDaemon(true);
        this.renderThread.start();
    }

    public void setVideoGenerator(@Nullable VideoGenerator<?> videoGenerator) {
        if (videoGenerator == null) {
            this.videoGenerator = null;
            this.renderBuffer = null;
            this.bufferedImage = null;
            return;
        }

        int width = videoGenerator.getImageWidth();
        int height = videoGenerator.getImageHeight();
        this.videoGenerator = videoGenerator;
        this.renderBuffer = new int[width][height];
        this.bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void setSystemController(@Nullable SystemController<?> systemController) {
        if (systemController == null) {
            if (this.inputHandler != null) {
                this.removeKeyListener(this.inputHandler);
            }
            return;
        }
        this.inputHandler = new InputHandler(systemController);
        SwingUtilities.invokeLater(() -> this.addKeyListener(inputHandler));
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
        if (this.videoGenerator == null) {
            return;
        }

        int w = this.getWidth();
        int h = this.getHeight();

        if (w == this.lastWidth && h == this.lastHeight) {
            return;
        }

        double logicalWidth = this.videoGenerator.getImageWidth();
        double logicalHeight = this.videoGenerator.getImageHeight();

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
        if (this.bufferedImage == null || this.videoGenerator == null) {
            return;
        }

        int[] pixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
        synchronized (renderBufferLock) {
            int displayHeight = this.videoGenerator.getImageHeight();
            int displayWidth = this.videoGenerator.getImageWidth();
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

    private static class InputHandler extends KeyAdapter {

        private final Map<Integer, KeyMapping> keyMappings = new HashMap<>();

        private InputHandler(SystemController<?> systemController) {
            Collection<KeyMapping> mappings = systemController.getKeyMappings();
            for (KeyMapping mapping : mappings) {
                keyMappings.put(KeyEvent.getExtendedKeyCodeForChar(mapping.getDefaultCodePoint()), mapping);
            }
        }

        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            for (Map.Entry<Integer, KeyMapping> mapping : this.keyMappings.entrySet()) {
                if (mapping.getKey() == keyCode) {
                    mapping.getValue().getKeyPressedCallback().run();
                }
            }
        }

        public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();
            for (Map.Entry<Integer, KeyMapping> mapping : this.keyMappings.entrySet()) {
                if (mapping.getKey() == keyCode) {
                    mapping.getValue().getKeyReleasedCallback().run();
                }
            }
        }

    }

}
