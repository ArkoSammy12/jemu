package io.github.arkosammy12.jemu.systems.gameboy;

import io.github.arkosammy12.jemu.systems.Bus;
import io.github.arkosammy12.jemu.systems.ReadWriteBus;

public class DMGBus implements ReadWriteBus {

    public DMGBus(GameBoyEmulator emulator) {

    }

    @Override
    public int getMemorySize() {
        return 0;
    }

    @Override
    public int getMemoryBoundsMask() {
        return 0;
    }

    @Override
    public int getByte(int address) {
        return 0;
    }

    @Override
    public int readByte(int address) {
        return 0;
    }

    @Override
    public void writeByte(int address, int value) {
        // TODO: REMEMBER TO MASK INPUT with 0xFF WHEN WRITING
    }

}
