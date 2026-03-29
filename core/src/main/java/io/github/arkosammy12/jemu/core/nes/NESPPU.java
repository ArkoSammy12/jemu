package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.VideoGenerator;

public class NESPPU<E extends NESEmulator> extends VideoGenerator<E> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;

    public NESPPU(E emulator) {
        super(emulator);
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return HEIGHT;
    }
}
