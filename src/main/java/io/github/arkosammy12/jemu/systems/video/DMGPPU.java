package io.github.arkosammy12.jemu.systems.video;

import io.github.arkosammy12.jemu.systems.gameboy.GameBoyEmulator;

public class DMGPPU<E extends GameBoyEmulator> extends Display<E> {

    public DMGPPU(E emulator) {
        super(emulator);

    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getImageWidth() {
        return 0;
    }

    @Override
    public int getImageHeight() {
        return 0;
    }

    @Override
    public void populateRenderBuffer(int[][] renderBuffer) {

    }

}
