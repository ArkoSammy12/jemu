package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.SuperChip11Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;

public class SuperChip11Display<E extends SuperChip11Emulator> extends SuperChip10Display<E> {

    public SuperChip11Display(E emulator) {
        super(emulator);
    }

    public void scrollDown(int scrollAmount) {
        for (int i = this.imageHeight - 1; i >= 0; i--) {
            int shiftedVerticalPosition = scrollAmount + i;
            if (shiftedVerticalPosition >= this.imageHeight) {
                continue;
            }
            for (int j = 0; j < this.imageWidth; j++) {
                this.bitplaneBuffer[(shiftedVerticalPosition * this.imageWidth) + j] = this.bitplaneBuffer[(i * this.imageWidth) + j];
            }
        }
        for (int y = 0; y < scrollAmount && y < this.imageHeight; y++) {
            for (int x = 0; x < this.imageWidth; x++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] = 0;
            }
        }
    }

    public void scrollRight(int scrollAmount) {
        for (int i = this.imageWidth - 1; i >= 0; i--) {
            int shiftedHorizontalPosition = i + scrollAmount;
            if (shiftedHorizontalPosition >= this.imageWidth) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + shiftedHorizontalPosition] = this.bitplaneBuffer[(y * this.imageWidth) + i];
            }
        }
        for (int x = 0; x < scrollAmount && x < this.imageWidth; x++) {
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] = 0;
            }
        }
    }

    public void scrollLeft(int scrollAmount) {
        for (int i = 0; i < this.imageWidth; i++) {
            int shiftedHorizontalPosition = i - scrollAmount;
            if (shiftedHorizontalPosition < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + shiftedHorizontalPosition] = this.bitplaneBuffer[(y * this.imageWidth) + i];
            }
        }
        for (int x = this.imageWidth - scrollAmount; x < this.imageWidth; x++) {
            if (x < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] = 0;
            }
        }
    }

    @Override
    public int draw(int spriteX, int spriteY, int spriteHeight, int indexRegister) {
        Chip8Bus bus = this.emulator.getBus();

        int displayWidth = this.getWidth();
        int displayHeight = this.getHeight();

        spriteX %= displayWidth;
        spriteY %= displayHeight;
        spriteHeight = spriteHeight < 1 ? 16 : spriteHeight;

        boolean doClipping = this.emulator.getSettings().doClipping();
        boolean draw16WideSprite = this.hires && spriteHeight >= 16;

        int sliceLength;
        int baseMask;
        if (draw16WideSprite) {
            sliceLength = 16;
            baseMask = BASE_SLICE_MASK_16;
        } else {
            sliceLength = 8;
            baseMask = BASE_SLICE_MASK_8;
        }

        int collisions = 0;
        for (int i = 0; i < spriteHeight; i++) {
            int sliceY = spriteY + i;
            if (sliceY >= displayHeight) {
                if (doClipping) {
                    if (this.hires) {
                        collisions++;
                    }
                    continue;
                } else {
                    sliceY %= displayHeight;
                }
            }

            int slice = draw16WideSprite
                    ? (bus.readByte(indexRegister + i * 2) << 8) | bus.readByte(indexRegister + (i * 2) + 1)
                    : bus.readByte(indexRegister + i);

            boolean sliceCollided = false;
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
                    sliceCollided |= this.bitplaneBuffer[index] == 0;
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

                    sliceCollided |= this.bitplaneBuffer[index0] == 0;
                    sliceCollided |= this.bitplaneBuffer[index1] == 0;
                }
            }
            if (!this.hires) {
                int x1 = (spriteX * 2) & 0x70;
                int x2 = Math.min(x1 + 32, displayWidth * 2);
                int scaledSliceY = sliceY * 2;
                for (int j = x1; j < x2; j++) {
                    this.bitplaneBuffer[((scaledSliceY + 1) * this.imageWidth) + j] = this.bitplaneBuffer[(scaledSliceY * this.imageWidth) + j];
                }
                if (sliceCollided) {
                    collisions = 1;
                }
            } else if (sliceCollided) {
                collisions++;
            }
        }
        return collisions;
    }

}
