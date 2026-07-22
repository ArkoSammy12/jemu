package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.Chip8XEmulator;

public class Chip8XDisplay<E extends Chip8XEmulator> extends Chip8Display<E> {

    private static final int[] BACKGROUND_COLORS = {
            0x000080,
            0x000000,
            0x008000,
            0X800000
    };

    private static final int[] FOREGROUND_COLORS = {
            0x181818,
            0xFF0000,
            0x0000FF,
            0xFF00FF,
            0x00FF00,
            0xFFFF00,
            0x00FFFF,
            0xFFFFFF
    };

    private final int[] frameBuffer;
    private final int[] foregroundColorIndexes = new int[64 * 32];
    private int backgroundColorIndex = 0;
    private boolean hiresColor = false;

    public Chip8XDisplay(E emulator) {
        super(emulator);
        this.frameBuffer = new int[this.imageWidth * this.imageHeight];

        // CHIP-8X self color test on startup
        for (int i = 0; i < 8; i++) {
            this.foregroundColorIndexes[i] = 2;
        }
    }

    @Override
    public int mapToRGB8(int frameBufferValue) {
        return frameBufferValue;
    }

    public void cycleBackgroundColor() {
        this.backgroundColorIndex = (this.backgroundColorIndex + 1) % BACKGROUND_COLORS.length;
    }

    private void setHiresColor(boolean hiresColor) {
        this.hiresColor = hiresColor;
    }

    public void drawColor(int vX, int vX1, int colorIndex, int N) {
        int displayWidth = this.getWidth();
        int displayHeight = this.getHeight();

        if (N > 0) {
            this.setHiresColor(true);
            int zoneX = vX & 0x38;
            for (int i = 0; i < N; i++) {
                int colorY = (vX1 + i) % displayHeight;
                for (int j = 0; j < 8; j++) {
                    this.foregroundColorIndexes[(colorY * 64) + ((j + zoneX) % displayWidth)] = colorIndex;
                }
            }
        } else {
            this.setHiresColor(false);
            int horizontalZoneFill = ((vX & 0xF0) >> 4) + 1;
            int zoneFillStartHorizontalOffset = vX & 0xF;
            int verticalZoneFill = ((vX1 & 0xF0) >> 4) + 1;
            int zoneFillStartVerticalOffset = vX1 & 0xF;
            for (int i = 0; i < verticalZoneFill; i++) {
                int zoneY = ((zoneFillStartVerticalOffset + i) * 4) % displayHeight;
                for (int j = 0; j < horizontalZoneFill; j++) {
                    int zoneX = ((zoneFillStartHorizontalOffset + j) * 8) % displayWidth;
                    for (int dx = 0; dx < 8; dx++) {
                        this.foregroundColorIndexes[(zoneY * 64) + ((zoneX + dx) % displayWidth)] = colorIndex;
                    }
                }
            }
        }
    }

    @Override
    public void onFrame() {
        if (this.hiresColor) {
            for (int y = 0; y < this.imageHeight; y++) {
                for (int x = 0; x < this.imageWidth; x++) {
                    this.frameBuffer[(y * this.imageWidth) + x] = this.bitplaneBuffer[(y * this.imageWidth) + x] != 0 ? FOREGROUND_COLORS[this.foregroundColorIndexes[(y * this.imageWidth) + x]] : BACKGROUND_COLORS[this.backgroundColorIndex];
                }
            }
        } else {
            for (int i = 0; i < 8; i++) {
                int zoneY = i * 4;
                for (int j = 0; j < 8; j++) {
                    int zoneX = j * 8;
                    int zoneColorIndex = this.foregroundColorIndexes[(zoneY * this.imageWidth) + zoneX];
                    for (int dy = 0; dy < 4; dy++) {
                        int y = zoneY + dy;
                        for (int dx = 0; dx < 8; dx++) {
                            int x = zoneX + dx;
                            this.frameBuffer[(y * this.imageWidth) + x] = this.bitplaneBuffer[(y * this.imageWidth) + x] != 0 ? FOREGROUND_COLORS[zoneColorIndex] : BACKGROUND_COLORS[this.backgroundColorIndex];
                        }
                    }
                }
            }
        }
        this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.frameBuffer));
    }

}
