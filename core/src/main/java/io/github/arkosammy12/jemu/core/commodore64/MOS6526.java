package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.Bus;

public class MOS6526 implements Bus {

    @Override
    public int readByte(int address) {
        address &= 0xF;

        return 0;
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0xF;

    }

    public void cycle() {

    }

    // TODO: Clock on VIC-II VBLANK
    public void clockTOD() {

    }

    // TODO: Read CNT input from emulator

}
