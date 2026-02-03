package io.github.arkosammy12.jemu.util;

import io.github.arkosammy12.jemu.systems.bus.ReadWriteBus;

public class FlatTestBus implements ReadWriteBus {

    private final int[] ram;
    private final int memoryBoundsMask;

    public FlatTestBus(int size, int memoryBoundsMask) {
        this.ram = new int[size];
        this.memoryBoundsMask = memoryBoundsMask;
    }

    @Override
    public void writeByte(int address, int value) {
        this.ram[address] = value & 0xFF;
    }

    @Override
    public int readByte(int address) {
        return this.ram[address];
    }

    @Override
    public int getMemorySize() {
        return this.ram.length;
    }

    @Override
    public int getMemoryBoundsMask() {
        return this.memoryBoundsMask;
    }

    @Override
    public int getByte(int address) {
        return this.readByte(address);
    }

}
