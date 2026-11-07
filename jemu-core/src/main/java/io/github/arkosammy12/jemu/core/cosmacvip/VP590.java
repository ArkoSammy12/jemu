package io.github.arkosammy12.jemu.core.cosmacvip;

import io.github.arkosammy12.jemu.core.hardware.CDP1861;

import java.util.Arrays;

public class VP590<E extends CosmacVIPEmulator> extends CDP1861<E> {

    private static final int[] BACKGROUND_COLORS = {
            0x000080,
            0x000000,
            0x008000,
            0X800000
    };

    private final byte[] colorRAM = new byte[256];
    private int backgroundColorIndex = 0;
    private boolean hiresColor = false;
    private boolean colorModeEnabled = false;

    public VP590(E emulator) {
        super(emulator);
    }

    @Override
    public void reset() {
        this.backgroundColorIndex = 0;
        this.colorModeEnabled = false;
        super.reset();
    }

    public void writeColorRAM(int address, int value) {
        this.colorModeEnabled = true;
        this.hiresColor = (address & (1 << 12)) != 0;
        this.colorRAM[address & (this.hiresColor ? 0xFF : 0xE7)] = (byte) (value & 7);
    }

    public int readColorRAM(int address) {
        return ((int) this.colorRAM[address & (this.hiresColor ? 0xFF : 0xE7)] & 0xFF) | 0xF0;
    }

    public void incrementBackgroundColorIndex() {
        this.backgroundColorIndex = (this.backgroundColorIndex + 1) % BACKGROUND_COLORS.length;
    }

    @Override
    protected void clearDisplay() {
        Arrays.fill(this.displayBuffer, BACKGROUND_COLORS[0]);
    }

    @Override
    protected int getPixelRGB(int dmaOutAddress, boolean bit) {
        if (!bit) {
            return BACKGROUND_COLORS[this.colorModeEnabled ? this.backgroundColorIndex : 0];
        } else {
            if (!this.colorModeEnabled) {
                return 0xFFFFFF;
            } else {
                int colorByte = this.readColorRAM(dmaOutAddress);
                int color = 0x000000;
                if ((colorByte & 1) != 0) {
                    color |= 0xFF0000;
                }
                if ((colorByte & 0b100) != 0) {
                    color |= 0x00FF00;
                }
                if ((colorByte & 0b10) != 0) {
                    color |= 0x0000FF;
                }
                return color;
            }
        }
    }

}