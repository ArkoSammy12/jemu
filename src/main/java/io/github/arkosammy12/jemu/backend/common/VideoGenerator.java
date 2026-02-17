package io.github.arkosammy12.jemu.backend.common;

public abstract class VideoGenerator<E extends Emulator> {

    protected final E emulator;

    protected final int imageWidth;
    protected final int imageHeight;

    public VideoGenerator(E emulator) {
        this.emulator = emulator;
        this.imageWidth = this.getImageWidth();
        this.imageHeight = this.getImageHeight();
    }

    public abstract int getImageWidth();

    public abstract int getImageHeight();

}
