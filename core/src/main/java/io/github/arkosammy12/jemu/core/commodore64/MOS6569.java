package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.VideoGenerator;

public class MOS6569 implements VideoGenerator {

    @Override
    public int getImageWidth() {
        return 0;
    }

    @Override
    public int getImageHeight() {
        return 0;
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return 0;
    }

    public void clockBusAccess() {

    }

    public void clockPixel() {

    }

    public boolean getIRQ() {
        return false;
    }

    public boolean getBA() {
        return false;
    }

    public boolean getAEC() {
        return false;
    }

}
