package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;

import java.awt.image.BufferStrategy;

import static io.github.arkosammy12.jemu.app.Jemu.tryJoinSafely;

public class DefaultSystemVideoDriver extends Canvas implements VideoDriver, Closeable {

    private final VideoGenerator videoGenerator;
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

    private int lastWidth;
    private int lastHeight;

    private double lastPixelAspectRatio;

    public DefaultSystemVideoDriver(VideoGenerator videoGenerator) {
        this.videoGenerator = videoGenerator;
        this.displayWidth = videoGenerator.getImageWidth();
        this.displayHeight = videoGenerator.getImageHeight();

        this.lastWidth = this.getWidth();
        this.lastHeight = this.getHeight();
        this.lastPixelAspectRatio = videoGenerator.getPixelAspectRatio();

        this.renderBuffer = new int[displayWidth * displayHeight];
        this.bufferedImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_RGB);

        this.renderThread = new Thread(this::renderLoop, "%s-render-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.renderThread.setDaemon(true);
        this.renderThread.start();
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
        double pixelAspectRatio = this.videoGenerator.getPixelAspectRatio();

        if (w == this.lastWidth && h == this.lastHeight && pixelAspectRatio == this.lastPixelAspectRatio) {
            return;
        }

        double logicalWidth = (double) this.displayWidth * pixelAspectRatio;

        double scale = Math.min((double) w / logicalWidth, (double) h / (double) this.displayHeight);

        double scaleX = scale * pixelAspectRatio;

        double scaledWidth  = (double) this.displayWidth * scaleX;
        double scaledHeight = (double) this.displayHeight * scale;

        double offsetX = ((double) w - scaledWidth) / 2.0;
        double offsetY = ((double) h - scaledHeight) / 2.0;

        this.drawTransform.setToIdentity();
        this.drawTransform.translate(offsetX, offsetY);
        this.drawTransform.scale(scaleX, scale);

        this.lastWidth = w;
        this.lastHeight = h;
        this.lastPixelAspectRatio = pixelAspectRatio;
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
        tryJoinSafely(this.renderThread);
    }

}