package io.github.arkosammy12.jemu.core.gameboycolor;

import io.github.arkosammy12.jemu.core.gameboy.DMGBus;

public class CGBBus<E extends GameBoyColorEmulator> extends DMGBus<E> {

    public CGBBus(E emulator) {
        super(emulator);
    }

    @Override
    protected int[][] createWorkRam() {
        return new int[8][0x1000];
    }

    // TODO: Adapt echo RAM to reflect ram banks

    @Override
    public int readByte(int address) {
        if (this.oamTransferInProgress) {
            return super.readByte(address);
        } else if (address >= WRAMX_START && address <= WRAMX_END) {
            return this.workRam[this.emulator.getMMIOBus().getWorkRamBank()][address - WRAMX_START];
        } else {
            return super.readByte(address);
        }
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        if (this.oamTransferInProgress) {
            super.writeByte(address, value);
            return;
        }
        if (address >= WRAMX_START && address <= WRAMX_END) {
            this.workRam[this.emulator.getMMIOBus().getWorkRamBank()][address - WRAMX_START] = value & 0xFF;
        } else {
            super.writeByte(address, value);
        }
    }

}
