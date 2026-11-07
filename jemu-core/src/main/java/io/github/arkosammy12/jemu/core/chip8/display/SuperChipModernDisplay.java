package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.SuperChipModernEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;

public class SuperChipModernDisplay<E extends SuperChipModernEmulator> extends SuperChip11Display<E> {

    public SuperChipModernDisplay(E emulator) {
        super(emulator);
    }

    @Override
    public void scrollDown(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        super.scrollDown(scrollAmount);
    }

    @Override
    public void scrollRight(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        super.scrollRight(scrollAmount);
    }

    @Override
    public void scrollLeft(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        super.scrollLeft(scrollAmount);
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public int draw(int spriteX, int spriteY, int spriteHeight, int indexRegister) {
        Chip8Bus bus = this.emulator.getBus();

        int displayWidth = this.getWidth();
        int displayHeight = this.getHeight();

        spriteX %= displayWidth;
        spriteY %= displayHeight;
        spriteHeight = spriteHeight < 1 ? 16 : spriteHeight;

        boolean doClipping = this.emulator.getHost().doClipping();
        boolean draw16WideSprite = spriteHeight >= 16;

        int sliceLength;
        int baseMask;
        if (draw16WideSprite) {
            sliceLength = 16;
            baseMask = BASE_SLICE_MASK_16;
        } else {
            sliceLength = 8;
            baseMask = BASE_SLICE_MASK_8;
        }

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

            int slice = draw16WideSprite
                    ? (bus.readByte(indexRegister + i * 2) << 8) | bus.readByte(indexRegister + (i * 2) + 1)
                    : bus.readByte(indexRegister + i);

            for (int j = 0, sliceMask = baseMask; j < sliceLength; j++, sliceMask >>>= 1) {
                int sliceX = spriteX + j;
                if (sliceX >= displayWidth) {
                    if (doClipping) {
                        break;
                    } else {
                        sliceX %= displayWidth;
                    }
                }
                if ((slice & sliceMask) == 0) {
                    continue;
                }
                if (this.hires) {
                    int index = (sliceY * this.imageWidth) + sliceX;
                    this.bitplaneBuffer[index] ^= 1;
                    if (this.bitplaneBuffer[index] == 0) {
                        collision = 1;
                    }
                } else {
                    int scaledSliceX = sliceX * 2;
                    int scaledSliceY = sliceY * 2;

                    int index0 = (scaledSliceY * this.imageWidth) + scaledSliceX;
                    int index1 = (scaledSliceY * this.imageWidth) + (scaledSliceX + 1);
                    int index2 = ((scaledSliceY + 1) * this.imageWidth) + scaledSliceX;
                    int index3 = ((scaledSliceY + 1) * this.imageWidth) + (scaledSliceX + 1);

                    this.bitplaneBuffer[index0] ^= 1;
                    this.bitplaneBuffer[index1] ^= 1;
                    this.bitplaneBuffer[index2] ^= 1;
                    this.bitplaneBuffer[index3] ^= 1;

                    if (this.bitplaneBuffer[index0] == 0 || this.bitplaneBuffer[index1] == 0) {
                        collision = 1;
                    }
                }
            }
        }
        return collision;
    }

}
