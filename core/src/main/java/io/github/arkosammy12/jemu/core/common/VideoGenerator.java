package io.github.arkosammy12.jemu.core.common;

public interface VideoGenerator {

    int getImageWidth();

    int getImageHeight();

    default double getPixelAspectRatio() {
        return 1.0;
    }

    int mapToRGB8(int frameBufferValue);

}
