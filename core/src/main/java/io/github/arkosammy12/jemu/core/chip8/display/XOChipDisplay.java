package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.XOChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;

public class XOChipDisplay<E extends XOChipEmulator> extends SuperChipModernDisplay<E> {

    private static final int BITPLANE_BASE_MASK = 1 << 3;

    protected int bitplanes = 1;

    public XOChipDisplay(E emulator) {
        super(emulator);
    }

    public void setBitplanes(int bitplanes) {
        this.bitplanes = bitplanes & 0xF;
    }

    public void scrollUp(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        for (int i = 0; i < this.imageHeight; i++) {
            int shiftedVerticalPosition = i - scrollAmount;
            if (shiftedVerticalPosition < 0) {
                continue;
            }
            for (int j = 0; j < this.imageWidth; j++) {
                int shiftedIndex = (shiftedVerticalPosition * this.imageWidth) + j;
                this.bitplaneBuffer[shiftedIndex] = (this.bitplaneBuffer[shiftedIndex] & ~this.bitplanes) | (this.bitplaneBuffer[(i * this.imageWidth) + j] & this.bitplanes);
            }
        }

        for (int y = this.imageHeight - scrollAmount; y < this.imageHeight; y++) {
            if (y < 0) {
                continue;
            }
            for (int x = 0; x < this.imageWidth; x++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] &= ~this.bitplanes;
            }
        }
    }

    @Override
    public void scrollDown(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        for (int i = this.imageHeight - 1; i >= 0; i--) {
            int shiftedVerticalPosition = scrollAmount + i;
            if (shiftedVerticalPosition >= this.imageHeight) {
                continue;
            }
            for (int j = 0; j < this.imageWidth; j++) {
                int shiftedIndex = (shiftedVerticalPosition * this.imageWidth) + j;
                this.bitplaneBuffer[shiftedIndex] = (this.bitplaneBuffer[shiftedIndex] & ~this.bitplanes) | (this.bitplaneBuffer[(i * this.imageWidth) + j] & this.bitplanes);
            }
        }
        for (int y = 0; y < scrollAmount && y < this.imageHeight; y++) {
            for (int x = 0; x < this.imageWidth; x++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] &= ~this.bitplanes;
            }
        }
    }

    @Override
    public void scrollRight(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        for (int i = this.imageWidth - 1; i >= 0; i--) {
            int shiftedHorizontalPosition = i + scrollAmount;
            if (shiftedHorizontalPosition >= this.imageWidth) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                int shiftedIndex = (y * this.imageWidth) + shiftedHorizontalPosition;
                this.bitplaneBuffer[shiftedIndex] = (this.bitplaneBuffer[shiftedIndex] & ~this.bitplanes) | (this.bitplaneBuffer[(y * this.imageWidth) + i] & this.bitplanes);
            }
        }
        for (int x = 0; x < scrollAmount && x < this.imageWidth; x++) {
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] &= ~this.bitplanes;
            }
        }
    }

    @Override
    public void scrollLeft(int scrollAmount) {
        if (!this.hires) {
            scrollAmount *= 2;
        }
        for (int i = 0; i < this.imageWidth; i++) {
            int shiftedHorizontalPosition = i - scrollAmount;
            if (shiftedHorizontalPosition < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                int shiftedIndex = (y * this.imageWidth) + shiftedHorizontalPosition;
                this.bitplaneBuffer[shiftedIndex] = (this.bitplaneBuffer[shiftedIndex] & ~this.bitplanes) | (this.bitplaneBuffer[(y * this.imageWidth) + i] & this.bitplanes);
            }
        }
        for (int x = this.imageWidth - scrollAmount; x < this.imageWidth; x++) {
            if (x < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.bitplaneBuffer[(y * this.imageWidth) + x] &= ~this.bitplanes;
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

        boolean collided = false;
        int planeIterator = 0;
        for (int bitPlaneMask = 1; bitPlaneMask <= BITPLANE_BASE_MASK; bitPlaneMask <<= 1) {
            if ((bitPlaneMask & this.bitplanes) == 0) {
                continue;
            }
            for (int i = 0; i < spriteHeight; i++, planeIterator++) {
                int sliceY = spriteY + i;
                if (sliceY >= displayHeight) {
                    if (doClipping) {
                        continue;
                    } else {
                        sliceY %= displayHeight;
                    }
                }

                int slice = draw16WideSprite
                        ? (bus.readByte(indexRegister + (planeIterator * 2)) << 8) | bus.readByte(indexRegister + (planeIterator * 2) + 1)
                        : bus.readByte(indexRegister + planeIterator);

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
                        collided |= this.drawPixelAtBitplanes(sliceX, sliceY, bitPlaneMask);
                    } else {
                        int scaledSliceX = sliceX * 2;
                        int scaledSliceY = sliceY * 2;
                        collided |= this.drawPixelAtBitplanes(scaledSliceX, scaledSliceY, bitPlaneMask);
                        collided |= this.drawPixelAtBitplanes(scaledSliceX + 1, scaledSliceY, bitPlaneMask);
                        this.drawPixelAtBitplanes(scaledSliceX, scaledSliceY + 1, bitPlaneMask);
                        this.drawPixelAtBitplanes(scaledSliceX + 1, scaledSliceY + 1, bitPlaneMask);
                    }
                }
            }
        }
        return collided ? 1 : 0;
    }

    protected boolean drawPixelAtBitplanes(int column, int row, int bitplaneMask) {
        int index = (row * this.imageWidth) + column;
        boolean collision = (this.bitplaneBuffer[index] & bitplaneMask) != 0;
        this.bitplaneBuffer[index] ^= bitplaneMask;
        return collision;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.bitplaneBuffer.length; i++) {
            this.bitplaneBuffer[i] &= ~this.bitplanes;
        }
    }

}
