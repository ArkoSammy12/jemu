package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;

public class Atari2600Bus<E extends Atari2600Emulator> implements Bus {

    private final E emulator;

    private int dataBus;

    public Atari2600Bus(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        address &= 0x1FFF;
        int ret;
        int cartridgeByte = this.emulator.getCartridge().readByte(address);
        if ((address & 0x1000) != 0) {
            ret = cartridgeByte;
        } else if ((address & 0x80) != 0) {
            ret = this.emulator.getPIA().readByte(address);
        } else {
            ret = this.emulator.getTIA().readByte(address);
        }
        this.dataBus = ret & 0xFF;
        return this.dataBus;
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x1FFF;
        value &= 0xFF;
        this.dataBus = value;
        this.emulator.getCartridge().writeByte(address, value);
        if ((address & 0x1000) == 0) {
            if ((address & 0x80) != 0) {
                this.emulator.getPIA().writeByte(address, value);
            } else {
                this.emulator.getTIA().writeByte(address, value);
            }
        }
    }

    public int getDataBus() {
        return this.dataBus;
    }

    public int combineWithDataBus(int value, int validBitsMask) {
        return (value & validBitsMask & 0xFF) | (this.dataBus & ~validBitsMask);
    }

}
