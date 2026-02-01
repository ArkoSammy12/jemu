package io.github.arkosammy12.jemu.systems;

import io.github.arkosammy12.jemu.ui.video.DisplayRenderer;
import io.github.arkosammy12.jemu.util.DisplayAngle;

import java.io.Closeable;

public abstract class Display<E extends Emulator> implements Closeable {

    protected final E emulator;
    private final DisplayRenderer displayRenderer;

    private final DisplayAngle displayAngle;
    protected final int imageWidth;
    protected final int imageHeight;

    public Display(E emulator) {
        this.emulator = emulator;
        this.displayAngle = emulator.getEmulatorSettings().getDisplayAngle();
        this.imageWidth = this.getImageWidth();
        this.imageHeight = this.getImageHeight();
        this.displayRenderer = new DisplayRenderer(this, emulator.getKeyAdapters());
    }

    public DisplayRenderer getDisplayRenderer() {
        return this.displayRenderer;
    }

    public DisplayAngle getDisplayAngle() {
        return this.displayAngle;
    }

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract int getImageWidth();

    public abstract int getImageHeight();

    public abstract void populateRenderBuffer(int[][] renderBuffer);

    public void flush() {
        this.displayRenderer.updateRenderBuffer();
    }

    @Override
    public void close() {
        this.displayRenderer.close();
    }

}
