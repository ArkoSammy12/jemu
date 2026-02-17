package io.github.arkosammy12.jemu.util;

import io.github.arkosammy12.jemu.backend.common.Bus;

public class FlatTestBus implements Bus {

    private final int[] ram;

    public FlatTestBus(int size) {
        this.ram = new int[size];
    }

    @Override
    public void writeByte(int address, int value) {
        this.ram[address] = value & 0xFF;
    }

    @Override
    public int readByte(int address) {
        return this.ram[address];
    }

}
