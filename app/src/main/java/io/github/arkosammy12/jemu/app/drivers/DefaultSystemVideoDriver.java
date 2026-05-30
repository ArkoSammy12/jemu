package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.adapters.JoypadAdapter;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;

import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component;

import java.awt.image.BufferStrategy;

public class DefaultSystemVideoDriver extends Canvas implements VideoDriver, Closeable {

    private final int[] renderBuffer;

    private final int displayWidth;
    private final int displayHeight;

    private final BufferedImage bufferedImage;
    private final AffineTransform drawTransform = new AffineTransform();

    private final Thread renderThread;
    private final Object renderLock = new Object();
    protected final Object renderBufferLock = new Object();

    private volatile boolean running = true;
    private boolean frameRequested = false;

    private final Thread joypadThread;
    private volatile boolean joypadRunning = true;
    private final JoypadAdapter joypadAdapter;

    private int lastWidth = -1;
    private int lastHeight = -1;

    public DefaultSystemVideoDriver(VideoGenerator<?> videoGenerator, KeyListener keyListener, JoypadAdapter joypadAdapter) {
        this.displayWidth = videoGenerator.getImageWidth();
        this.displayHeight = videoGenerator.getImageHeight();

        this.renderBuffer = new int[displayWidth * displayHeight];
        this.bufferedImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_RGB);

        this.addKeyListener(keyListener);
        this.joypadAdapter = joypadAdapter;

        this.joypadThread = new Thread(this::joypadLoop, "%s-joypad-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.joypadThread.setDaemon(true);
        this.joypadThread.start();

        this.renderThread = new Thread(this::renderLoop, "%s-render-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.renderThread.setDaemon(true);
        this.renderThread.start();
    }

    private void joypadLoop() {
        Event event = new Event();

        while (joypadRunning) {
            // Leave this in the loop - presumably it will populate controllers going online
            // after application has started?
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

            if(controllers.length > 0)
            {
                // Only poll the first controller for now, as this is a hard-coded POC
                // Remember that there could be multiple controllers for release
                var controller = controllers[0];

                if (controller.poll())
                {
                    EventQueue queue = controller.getEventQueue();

                    while (queue.getNextEvent(event)) {
                        this.joypadAdapter.joypadEvent(event);
                    }
                }
            }

            try {
                Thread.sleep(5); // test
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public void outputFrame(int[] rgb) {
        synchronized (this.renderBufferLock) {
            System.arraycopy(rgb, 0, this.renderBuffer, 0, rgb.length);
        }
    }

    public void requestFrame() {
        synchronized (this.renderLock) {
            this.frameRequested = true;
            this.renderLock.notify();
        }
    }

    private void updateTransformIfNeeded() {
        int w = this.getWidth();
        int h = this.getHeight();

        if (w == this.lastWidth && h == this.lastHeight) {
            return;
        }

        double scale = Math.min((double) w / (double) this.displayWidth, (double) h / (double) this.displayHeight);

        double scaledWidth = (double) this.displayWidth * scale;
        double scaledHeight = (double) this.displayHeight * scale;

        double offsetX = ((double) w - scaledWidth) / 2.0;
        double offsetY = ((double) h - scaledHeight) / 2.0;

        this.drawTransform.setToIdentity();
        this.drawTransform.translate(offsetX, offsetY);
        this.drawTransform.scale(scale, scale);

        this.lastWidth = w;
        this.lastHeight = h;
    }

    private void renderLoop() {
        while (this.running) {
            try {
                synchronized (this.renderLock) {
                    while (this.running && !this.frameRequested) {
                        try {
                            this.renderLock.wait();
                        } catch (InterruptedException _) {}
                    }
                    this.frameRequested = false;
                }
                this.renderFrame();
            } catch (Exception e) {
                Logger.warn("Render thread encountered an unexpected error, continuing: {}", e.getMessage());
            }
        }
    }

    private void renderFrame() {
        BufferStrategy bufferStrategy = this.getBufferStrategy();
        if (bufferStrategy == null) {
            try {
                this.createBufferStrategy(3);
            } catch (Exception e) {
                Logger.warn("Failed to create buffer strategy: {}", e.getMessage());
            }
            return;
        }
        this.updateTransformIfNeeded();
        synchronized (this.renderBufferLock) {
            System.arraycopy(this.renderBuffer, 0, ((DataBufferInt) this.bufferedImage.getRaster().getDataBuffer()).getData(), 0, this.renderBuffer.length);
        }
        try {
            Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(this.bufferedImage, this.drawTransform, null);
            g.dispose();
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();
        } catch (Exception e) {
            Logger.warn("Failed to render frame to canvas: {}", e.getMessage());
        }
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

        this.joypadRunning = false;
        if (this.joypadThread != null) {
            try {
                this.joypadThread.join();
            } catch (InterruptedException _) {}
        }
    }

}