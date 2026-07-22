package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.HyperWaveChip64Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;

public class HyperWaveChip64Display<E extends HyperWaveChip64Emulator> extends XOChipDisplay<E> {

    private final int[] frameBuffer;
    private final int[] colorPalette = new int[16];
    private DrawingMode drawingMode = DrawingMode.XOR;

    public HyperWaveChip64Display(E emulator) {
        super(emulator);
        this.frameBuffer = new int[this.imageWidth * this.imageHeight];
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return frameBufferValue;
    }

    public void setDrawingMode(DrawingMode drawingMode) {
        this.drawingMode = drawingMode;
    }

    public void loadPalette(int index, int indexRegister) {
        Chip8Bus bus = this.emulator.getBus();
        this.colorPalette[index & 0xF] = (bus.readByte(indexRegister) << 16) | (bus.readByte(indexRegister + 1) << 8) | bus.readByte(indexRegister + 2);
    }

    public void invert() {
        for (int i = 0; i < this.bitplaneBuffer.length; i++) {
            this.bitplaneBuffer[i] ^= this.bitplanes;
        }
    }

    @Override
    protected boolean drawPixelAtBitplanes(int column, int row, int bitplaneMask) {
        int index = (row * this.imageWidth) + column;
        boolean collision = (this.bitplaneBuffer[index] & bitplaneMask) != 0;
        switch (this.drawingMode) {
            case OR -> this.bitplaneBuffer[index] |= bitplaneMask;
            case SUBTRACT -> this.bitplaneBuffer[index] &= ~bitplaneMask;
            case XOR -> this.bitplaneBuffer[index] ^= bitplaneMask;
        }
        return collision;
    }

    @Override
    public void onFrame() {
        for (int i = 0; i < this.frameBuffer.length; i++) {
            this.frameBuffer[i] = this.colorPalette[this.bitplaneBuffer[i] & 0xF];
        }
        this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.frameBuffer));
    }

    public enum DrawingMode {
        OR,
        SUBTRACT,
        XOR
    }

}
