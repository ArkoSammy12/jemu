package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.MegaChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.interpreters.MegaChipInterpreter;

public class MegaChipDisplay<E extends MegaChipEmulator> extends SuperChip11Display<E> {

    private static final int BUFFER_WIDTH = 256;
    private static final int BUFFER_HEIGHT = 256;

    private final int[] frameBuffer;
    private final int[] backBuffer = new int[BUFFER_WIDTH * BUFFER_HEIGHT];
    private final int[] indexBuffer = new int[BUFFER_WIDTH * BUFFER_HEIGHT];
    private final int[] frontBuffer = new int[BUFFER_WIDTH * BUFFER_HEIGHT];
    private final int[] colorPalette = new int[256];
    private int spriteWidth;
    private int spriteHeight;
    private int screenAlpha = 0xFF;
    private int collisionColorIndex = 0;
    private BlendMode blendMode = BlendMode.BLEND_NORMAL;

    private boolean scrollTriggered = false;

    public MegaChipDisplay(E emulator) {
        super(emulator);
        this.frameBuffer = new int[this.getImageWidth() * this.getImageHeight()];
        this.colorPalette[0] = 0x000000;
        this.colorPalette[255] = 0xFFFFFF;
    }

    @Override
    public int getImageWidth() {
        return 256;
    }

    @Override
    public int getImageHeight() {
        return 192;
    }

    @Override
    public int getWidth() {
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            return super.getWidth();
        }
        return 256;
    }

    @Override
    public int getHeight() {
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            return super.getHeight();
        }
        return 256;
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return frameBufferValue;
    }

    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
    }

    public void setSpriteWidth(int value) {
        value &= 0xFF;
        this.spriteWidth = value < 1 ? 256 : value;
    }

    public void setSpriteHeight(int value) {
        value &= 0xFF;
        this.spriteHeight = value < 1 ? 256 : value;
    }

    public void setAlpha(int value) {
        this.screenAlpha = value & 0xFF;
    }

    public void setCollisionColor(int value) {
        this.collisionColorIndex = value & 0xFF;
    }

    public void loadPalette(int indexRegister, int entries) {
        Chip8Bus bus = this.emulator.getBus();
        for (int i = 0; i < entries; i++) {
            this.colorPalette[i + 1] = (bus.readByte(indexRegister + (i * 4)) << 24) | (bus.readByte(indexRegister + (i * 4) + 1) << 16) | (bus.readByte(indexRegister + (i * 4) + 2) << 8) | bus.readByte(indexRegister + (i * 4) + 3);
        }
    }

    public void flushBackBuffer() {
        this.scrollTriggered = false;
        System.arraycopy(this.backBuffer, 0, this.frontBuffer, 0, this.backBuffer.length);
    }

    public void scrollUp(int scrollAmount) {
        this.scrollTriggered = true;
        for (int i = 0; i < this.imageHeight; i++) {
            int shiftedVerticalPosition = i - scrollAmount;
            if (shiftedVerticalPosition < 0) {
                continue;
            }
            for (int j = 0; j < this.imageWidth; j++) {
                this.frontBuffer[(shiftedVerticalPosition * BUFFER_WIDTH) + j] = this.frontBuffer[(i * BUFFER_WIDTH) + j];
            }
        }
        for (int y = this.imageHeight - scrollAmount; y < this.imageHeight; y++) {
            if (y < 0) {
                continue;
            }
            for (int x = 0; x < this.imageWidth; x++) {
                this.frontBuffer[(y * BUFFER_WIDTH) + x] = 0x000000;
            }
        }
    }

    @Override
    public void scrollDown(int scrollAmount) {
        this.scrollTriggered = true;
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            super.scrollDown(scrollAmount);
            return;
        }
        for (int i = this.imageHeight - 1; i >= 0; i--) {
            int shiftedVerticalPosition = scrollAmount + i;
            if (shiftedVerticalPosition >= this.imageHeight) {
                continue;
            }
            for (int j = 0; j < this.imageWidth; j++) {
                this.frontBuffer[(shiftedVerticalPosition * BUFFER_WIDTH) + j] = this.frontBuffer[(i * BUFFER_WIDTH) + j];
            }
        }
        for (int y = 0; y < scrollAmount && y < this.imageHeight; y++) {
            for (int x = 0; x < this.imageWidth; x++) {
                this.frontBuffer[(y * BUFFER_WIDTH) + x] = 0x000000;
            }
        }
    }

    @Override
    public void scrollRight(int scrollAmount) {
        this.scrollTriggered = true;
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            super.scrollRight(scrollAmount);
            return;
        }
        for (int i = this.imageWidth - 1; i >= 0; i--) {
            int shiftedHorizontalPosition = i + 4;
            if (shiftedHorizontalPosition >= this.imageWidth) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.frontBuffer[(y * this.imageWidth) + shiftedHorizontalPosition] = this.frontBuffer[(y * this.imageWidth) + i];
            }
        }
        for (int x = 0; x < 4 && x < this.imageWidth; x++) {
            for (int y = 0; y < this.imageHeight; y++) {
                this.frontBuffer[(y * BUFFER_WIDTH) + x] = 0x000000;
            }
        }
    }

    @Override
    public void scrollLeft(int scrollAmount) {
        this.scrollTriggered = true;
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            super.scrollLeft(scrollAmount);
            return;
        }
        for (int i = 0; i < this.imageWidth; i++) {
            int shiftedHorizontalPosition = i - 4;
            if (shiftedHorizontalPosition < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.frontBuffer[(y * this.imageWidth) + shiftedHorizontalPosition] = this.frontBuffer[(y * this.imageWidth) + i];
            }
        }
        for (int x = this.imageWidth - 4; x < this.imageWidth; x++) {
            if (x < 0) {
                continue;
            }
            for (int y = 0; y < this.imageHeight; y++) {
                this.frontBuffer[(y * BUFFER_WIDTH) + x] = 0x000000;
            }
        }
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public int draw(int spriteX, int spriteY, int spriteHeight, int indexRegister) {
        MegaChipInterpreter<?> interpreter = this.emulator.getInterpreter();
        Chip8Bus bus = this.emulator.getBus();

        int displayWidth = this.getWidth();
        int displayHeight = this.getHeight();

        spriteX %= displayWidth;
        spriteY %= displayHeight;

        boolean megaModeEnabled = interpreter.isMegaModeEnabled();
        boolean doClipping = this.emulator.getHost().doClipping();

        int collision = 0;
        if (interpreter.isPointingToFontSprite() || !megaModeEnabled) {
            spriteHeight = spriteHeight < 1 ? 16 : spriteHeight;

            boolean draw16WideSprite = megaModeEnabled ? spriteHeight >= 16 : this.hires && spriteHeight >= 16;

            int sliceLength;
            int baseMask;
            if (draw16WideSprite) {
                sliceLength = 16;
                baseMask = BASE_SLICE_MASK_16;
            } else {
                sliceLength = 8;
                baseMask = BASE_SLICE_MASK_8;
            }

            if (megaModeEnabled) {
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
                        // Use hardcoded opaque white for drawing font pixels, and set the index buffer to 255
                        int index = (sliceY * BUFFER_WIDTH) + sliceX;
                        this.backBuffer[index] = 0xFFFFFFFF;
                        this.indexBuffer[index] = 255;
                    }
                }
            } else {
                for (int i = 0; i < spriteHeight; i++) {
                    int sliceY = spriteY + i;
                    if (sliceY >= displayHeight) {
                        break;
                    }
                    int slice = draw16WideSprite
                            ? (bus.readByte(indexRegister + i * 2) << 8) | bus.readByte(indexRegister + (i * 2) + 1)
                            : bus.readByte(indexRegister + i);
                    for (int j = 0, sliceMask = baseMask; j < sliceLength; j++, sliceMask >>>= 1) {
                        int sliceX = spriteX + j;
                        if (sliceX >= displayWidth) {
                            break;
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
            }
        } else {
            spriteHeight = this.spriteHeight;
            for (int i = 0; i < spriteHeight; i++) {
                int pixelY = spriteY + i;
                if (pixelY >= displayHeight) {
                    if (doClipping) {
                        break;
                    } else {
                        pixelY %= displayHeight;
                    }
                }
                int base = i * this.spriteWidth;
                for (int j = 0; j < this.spriteWidth; j++) {
                    int pixelX = spriteX + j;
                    if (pixelX >= displayWidth) {
                        if (doClipping) {
                            break;
                        } else {
                            pixelX %= displayWidth;
                        }
                    }
                    int pixelColorIndex = bus.readByte(indexRegister + base + j);
                    if (pixelColorIndex == 0) {
                        continue;
                    }
                    int pixelBufferIndex = (pixelY * BUFFER_WIDTH) + pixelX;
                    if (this.indexBuffer[pixelBufferIndex] == this.collisionColorIndex && this.colorPalette[pixelColorIndex] != 0) {
                        collision = 1;
                    }
                    this.backBuffer[pixelBufferIndex] = switch (this.blendMode) {
                        case BLEND_NORMAL -> this.colorPalette[pixelColorIndex];
                        case BLEND_25 -> blendAlpha(this.colorPalette[pixelColorIndex], this.backBuffer[pixelBufferIndex], 64);
                        case BLEND_50 -> blendAlpha(this.colorPalette[pixelColorIndex], this.backBuffer[pixelBufferIndex], 128);
                        case BLEND_75 -> blendAlpha(this.colorPalette[pixelColorIndex], this.backBuffer[pixelBufferIndex], 192);
                        case BLEND_ADD -> addColors(this.colorPalette[pixelColorIndex], this.backBuffer[pixelBufferIndex]);
                        case BLEND_MULTIPLY -> multiplyColors(this.colorPalette[pixelColorIndex], this.backBuffer[pixelBufferIndex]);
                    };
                    this.indexBuffer[pixelBufferIndex] = pixelColorIndex;
                }
            }
        }
        return collision;
    }

    @Override
    public void clear() {
        this.flushBackBuffer();
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            super.clear();
            return;
        }
        for (int i = 0; i < this.backBuffer.length; i++) {
            this.backBuffer[i] = this.colorPalette[0];
            this.indexBuffer[i] = 0;
        }
    }

    @Override
    public void onFrame() {
        if (this.emulator.getInterpreter().isMegaModeEnabled()) {
            for (int y = 0; y < imageHeight; y++) {
                for (int x = 0; x < imageWidth; x++) {
                    int pixel = 0xFF000000;
                    if (scrollTriggered) {
                        int back = this.backBuffer[(y * this.imageWidth) + x];
                        if ((back & 0xFF000000) != 0) {
                            pixel = back;
                        }
                    }
                    int front = this.frontBuffer[(y * this.imageWidth) + x];
                    if ((front & 0xFF000000) != 0) {
                        pixel = front;
                    }
                    frameBuffer[(y * this.imageWidth) + x] = blendAlpha(pixel, 0xFF000000, this.screenAlpha) & 0xFFFFFF;
                }
            }
        } else {
            for (int y = 0; y < this.imageHeight; y++) {
                for (int x = 0; x < this.imageWidth; x++) {
                    this.frameBuffer[(y * this.imageWidth) + x] = 0x000000;
                }
            }
            int displayWidth = super.getImageWidth();
            int displayHeight = super.getImageHeight();
            int xScale = 2;
            int yScale = 2;
            int yOffset = (this.imageHeight - displayHeight * yScale) / 2;
            for (int sy = 0; sy < displayHeight; sy++) {
                int baseY = yOffset + sy * yScale;
                for (int sx = 0; sx < displayWidth; sx++) {
                    int color = this.emulator.getHost().getColorPalette().getRGB8ForIndex(this.bitplaneBuffer[(sy * this.imageWidth) + sx] & 0xF);
                    int baseX = sx * xScale;
                    for (int dy = 0; dy < yScale; dy++) {
                        for (int dx = 0; dx < xScale; dx++) {
                            this.frameBuffer[((baseY + dy) * this.imageWidth) + (baseX + dx)] = color;
                        }
                    }
                }
            }
        }
        this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.frameBuffer));
    }

    private static int blendAlpha(int src, int dst, int alpha) {
        int invAlpha = 255 - alpha;
        return 0xFF000000
                | (((((src >>> 16) & 0xFF) * alpha + ((dst >>> 16) & 0xFF) * invAlpha) / 255) << 16)
                | (((((src >>> 8) & 0xFF) * alpha + ((dst >>> 8) & 0xFF) * invAlpha) / 255) << 8)
                | (((src & 0xFF) * alpha + (dst & 0xFF) * invAlpha) / 255);
    }

    private static int addColors(int src, int dst) {
        return 0xFF000000
                | ((Math.min(((src >>> 16) & 0xFF) + ((dst >>> 16) & 0xFF), 255)) << 16)
                | ((Math.min(((src >>> 8) & 0xFF) + ((dst >>> 8) & 0xFF), 255)) << 8)
                | Math.min((src & 0xFF) + (dst & 0xFF), 255);
    }

    private static int multiplyColors(int src, int dst) {
        return 0xFF000000
                | (((((src >>> 16) & 0xFF) * ((dst >>> 16) & 0xFF)) / 255) << 16)
                | (((((src >>> 8) & 0xFF) * ((dst >>> 8) & 0xFF)) / 255) << 8)
                | (((src & 0xFF) * (dst & 0xFF)) / 255);
    }

    public enum BlendMode {
        BLEND_NORMAL,
        BLEND_25,
        BLEND_50,
        BLEND_75,
        BLEND_ADD,
        BLEND_MULTIPLY
    }

}
