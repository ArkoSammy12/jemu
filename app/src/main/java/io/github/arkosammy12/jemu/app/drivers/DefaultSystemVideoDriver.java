package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.util.FrameRequesterVideoEvent;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.drivers.VideoDriver;
import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.core.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.SystemDisplayComponent;
import org.jetbrains.annotations.NotNull;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.Closeable;

import java.awt.image.BufferStrategy;
import java.util.function.DoubleSupplier;

import static io.github.arkosammy12.jemu.app.Jemu.tryJoinSafely;

public class DefaultSystemVideoDriver extends Canvas implements VideoDriver, SystemDisplayComponent, Closeable {

    private final VideoGenerator videoGenerator;
    private final int[] frameBuffer;

    private final int displayWidth;
    private final int displayHeight;

    private final BufferedImage bufferedImage;
    private final AffineTransform drawTransform = new AffineTransform();
    private final ScaleSupplier scaleSupplier;

    private final Thread renderThread;
    private final Object renderLock = new Object();
    protected final Object renderBufferLock = new Object();

    private volatile boolean running = true;
    private boolean frameRequested = false;

    private ScaleSupplier currentScaleSupplier;
    private DoubleSupplier pixelAspectRatioSupplier;

    private int lastWidth;
    private int lastHeight;

    private double lastPixelAspectRatio;
    private volatile boolean forceTransformUpdate = false;

    public DefaultSystemVideoDriver(Jemu jemu, VideoGenerator videoGenerator) {
        this.videoGenerator = videoGenerator;
        this.displayWidth = videoGenerator.getImageWidth();
        this.displayHeight = videoGenerator.getImageHeight();
        this.scaleSupplier = (windowWidth, windowHeight, logicalWidth, logicalHeight) -> Math.min(windowWidth / logicalWidth, windowHeight / logicalHeight);

        this.setScaleSupplier(jemu.getMainWindow().getConfigurations().getSettings().getVideoSettings().getUseIntegerScaling());
        this.setPixelAspectRatioSupplier(jemu.getMainWindow().getConfigurations().getSettings().getVideoSettings().getAspectRatio());

        this.lastWidth = this.getWidth();
        this.lastHeight = this.getHeight();
        this.lastPixelAspectRatio = this.pixelAspectRatioSupplier.getAsDouble();

        this.frameBuffer = new int[displayWidth * displayHeight];
        this.bufferedImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_RGB);

        this.renderThread = new Thread(this::renderLoop, "%s-render-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.renderThread.setDaemon(true);
        this.renderThread.start();
    }

    @Override
    public void outputFrame(int[] frameBuffer) {
        synchronized (this.renderBufferLock) {
            System.arraycopy(frameBuffer, 0, this.frameBuffer, 0, this.frameBuffer.length);
        }
    }

    @Override
    public int getSystemDisplayWidth() {
        return this.displayWidth;
    }

    @Override
    public int getSystemDisplayHeight() {
        return this.displayHeight;
    }

    @Override
    @NotNull
    public Component getComponent() {
        return this;
    }

    public void requestFrame() {
        this.requestFrame(false);
    }

    private void requestFrame(boolean forceTransformUpdate) {
        this.forceTransformUpdate = forceTransformUpdate;
        synchronized (this.renderLock) {
            this.frameRequested = true;
            this.renderLock.notify();
        }
    }

    public void onVideoSettingChangedEvent(VideoSettingChangedEvent videoSettingChangedEvent) {
        switch (videoSettingChangedEvent) {
            case UseIntegerScalingSettingChangedEvent useIntegerScalingSettingChangedEvent -> {
                this.setScaleSupplier(useIntegerScalingSettingChangedEvent.useIntegerScaling());
                this.requestFrame(true);
            }
            case AspectRatioSettingChangedEvent aspectRatioSettingChangedEvent -> {
                this.setPixelAspectRatioSupplier(aspectRatioSettingChangedEvent.getAspectRatio());
                this.requestFrame(true);
            }
            case null, default -> {}
        }
        if (videoSettingChangedEvent instanceof FrameRequesterVideoEvent frameRequesterVideoEvent) {
            this.requestFrame(frameRequesterVideoEvent.invalidatesDisplay());
        }
    }

    private void setScaleSupplier(boolean useIntegerScaling) {
        if (useIntegerScaling) {
            this.currentScaleSupplier = (windowWidth, windowHeight, logicalWidth, logicalHeight) -> Math.max(1, Math.floor(this.scaleSupplier.getScale(windowWidth, windowHeight, logicalWidth, logicalHeight)));
        } else {
            this.currentScaleSupplier = this.scaleSupplier;
        }
    }

    private void setPixelAspectRatioSupplier(VideoSettings.AspectRatio aspectRatio) {
        if (aspectRatio == VideoSettings.AspectRatio.AUTO) {
            this.pixelAspectRatioSupplier = this.videoGenerator::getPixelAspectRatio;
        } else {
            this.pixelAspectRatioSupplier = aspectRatio::getPixelAspectRatio;
        }
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
                if (this.running) {
                    this.renderFrame();
                }
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
        int[] dataBufferInt = ((DataBufferInt) this.bufferedImage.getRaster().getDataBuffer()).getData();
        synchronized (this.renderBufferLock) {
            for (int i = 0; i < this.frameBuffer.length; i++) {
                dataBufferInt[i] = this.videoGenerator.mapToRGB8(this.frameBuffer[i]);
            }
        }
        try {
            Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g.drawImage(this.bufferedImage, this.drawTransform, null);
            g.dispose();
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();
        } catch (Exception e) {
            Logger.warn("Failed to render frame to canvas: {}", e.getMessage());
        }
    }

    private void updateTransformIfNeeded() {
        int w = this.getWidth();
        int h = this.getHeight();
        double pixelAspectRatio = this.pixelAspectRatioSupplier.getAsDouble();

        boolean forceUpdate = this.forceTransformUpdate;
        this.forceTransformUpdate = false;

        if (!forceUpdate && w == this.lastWidth && h == this.lastHeight && pixelAspectRatio == this.lastPixelAspectRatio) {
            return;
        }

        double logicalWidth = (double) this.displayWidth * pixelAspectRatio;

        double scale = this.currentScaleSupplier.getScale(w, h, logicalWidth, this.displayHeight);

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

    @Override
    public void close() {
        this.running = false;
        synchronized (this.renderLock) {
            this.renderLock.notifyAll();
        }
        tryJoinSafely(this.renderThread);
    }

    private interface ScaleSupplier {

        double getScale(double windowWidth, double windowHeight, double logicalWidth, double logicalHeight);

    }

}