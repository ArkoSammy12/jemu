package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Chip8Display<E extends Chip8Emulator> implements VideoGenerator {

    public static final int BASE_SLICE_MASK_8 = 1 << 7;

    protected final E emulator;
    protected final int imageWidth;
    protected final int imageHeight;
    protected final int[] bitplaneBuffer;

    public Chip8Display(E emulator) {
        this.emulator = emulator;
        this.imageWidth = this.getImageWidth();
        this.imageHeight = this.getImageHeight();
        this.bitplaneBuffer = new int[this.imageWidth * this.imageHeight];
    }

    @Override
    public int getImageWidth() {
        return 64;
    }

    @Override
    public int getImageHeight() {
        return 32;
    }

    public int getWidth() {
        return 64;
    }

    public int getHeight() {
        return 32;
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return this.emulator.getHost().getColorPalette().getRGB8ForIndex(frameBufferValue);
    }

    @Nullable
    public DisplayOrientation getDisplayOrientation() {
        return DisplayOrientation.DEG_0;
    }

    public int draw(int spriteX, int spriteY, int spriteHeight, int indexRegister) {
        Chip8Bus bus = this.emulator.getBus();

        int displayWidth = this.getWidth();
        int displayHeight = this.getHeight();

        spriteX %= displayWidth;
        spriteY %= displayHeight;

        boolean doClipping = this.emulator.getSettings().doClipping();

        int collision = 0;
        for (int i = 0; i < spriteHeight; i++) {
            int sliceY = spriteY + i;
            if (sliceY >= displayHeight) {
                if (doClipping) {
                    break;
                } else {
                    sliceY %= displayHeight;
                }
            }
            int slice = bus.readByte(indexRegister + i);
            for (int j = 0, sliceMask = BASE_SLICE_MASK_8; j < 8; j++, sliceMask >>>= 1) {
                int sliceX = spriteX + j;
                if (sliceX >= displayWidth) {
                    if (doClipping) {
                        break;
                    } else {
                        sliceX %= displayWidth;
                    }
                }
                if ((slice & sliceMask) != 0) {
                    int index = (sliceY * this.imageWidth) + sliceX;
                    this.bitplaneBuffer[index] ^= 1;
                    if (this.bitplaneBuffer[index] == 0) {
                        collision = 1;
                    }
                }
            }
        }
        return collision;
    }

    public void clear() {
        Arrays.fill(this.bitplaneBuffer, 0);
    }

    public void onFrame() {
        this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.bitplaneBuffer));
    }

}
