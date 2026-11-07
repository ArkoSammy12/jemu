package io.github.arkosammy12.jemu.core.common;

public interface VideoGenerator {

    int getImageWidth();

    int getImageHeight();

    default double getPixelAspectRatio() {
        return 1.0;
    }

    default DisplayOrientation getDisplayOrientation() {
        return DisplayOrientation.DEG_0;
    }

    int mapToRGB8(int frameBufferValue);

    enum DisplayOrientation {
        DEG_0,
        DEG_90,
        DEG_180,
        DEG_270
    }

}
