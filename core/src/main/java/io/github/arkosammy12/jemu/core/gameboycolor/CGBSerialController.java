package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.DMGSerialController;

public class CGBSerialController<E extends GameBoyColorEmulator> extends DMGSerialController<E> {

    public CGBSerialController(E emulator) {
        super(emulator);
    }

    @Override
    public int readByte(int address) {
        if (address == SC_ADDR) {
            return (this.serialControl & 0x7F) | (this.transferring ? 1 << 7 : 0) | (this.emulator.isDMGCompatibilityMode() ? 0b01111110 : 0b01111100);
        } else {
            return super.readByte(address);
        }
    }

    @Override
    protected boolean getClockSpeed() {
        if (this.emulator.isDMGCompatibilityMode()) {
            return false;
        } else {
            return (this.serialControl & (1 << 1)) != 0;
        }
    }

}
