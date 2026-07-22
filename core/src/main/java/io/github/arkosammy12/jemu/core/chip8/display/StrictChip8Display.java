package io.github.arkosammy12.jemu.core.chip8.display;

import io.github.arkosammy12.jemu.core.chip8.StrictChip8Emulator;
import io.github.arkosammy12.jemu.core.chip8.bus.StrictChip8Bus;

public final class StrictChip8Display extends Chip8Display<StrictChip8Emulator> {

    private final int[] frameBuffer;

    public StrictChip8Display(StrictChip8Emulator emulator) {
        super(emulator);
        this.frameBuffer = new int[this.imageWidth * this.imageHeight];
    }

    public boolean drawPixel(int column, int row) {
        StrictChip8Bus bus = this.emulator.getBus();
        bus.drawDisplayPixel(column, row);
        return !bus.getDisplayPixel(column, row);
    }

    @Override
    public void clear() {
        StrictChip8Bus bus = this.emulator.getBus();
        for (int i = 0; i < 256; i++) {
            bus.writeByte(StrictChip8Bus.DISPLAY_OFFSET + i, 0);
        }
    }

    @Override
    public void onFrame() {
        StrictChip8Bus bus = this.emulator.getBus();
        for (int y = 0; y < this.imageHeight; y++) {
            for (int x = 0; x < this.imageWidth; x++) {
                this.frameBuffer[(y * this.imageWidth) + x] = bus.getDisplayPixel(x, y) ? 1 : 0;
            }
        }
        this.emulator.getHost().getVideoDriver().ifPresent(videoDriver -> videoDriver.outputFrame(this.frameBuffer));
    }

}
