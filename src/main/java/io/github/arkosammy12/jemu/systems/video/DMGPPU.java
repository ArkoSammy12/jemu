package io.github.arkosammy12.jemu.systems.video;

import io.github.arkosammy12.jemu.systems.GameBoyEmulator;

import java.util.Arrays;

public class DMGPPU<E extends GameBoyEmulator> extends Display<E> {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;

    public DMGPPU(E emulator) {
        super(emulator);
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return HEIGHT;
    }

    @Override
    public void populateRenderBuffer(int[][] renderBuffer) {
        for (int[] ints : renderBuffer) {
            Arrays.fill(ints, 0xFF000000);
        }
    }

}
