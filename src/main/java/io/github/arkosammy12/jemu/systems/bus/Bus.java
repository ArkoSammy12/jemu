package io.github.arkosammy12.jemu.systems.bus;

public interface Bus {

    void writeByte(int address, int value);

    int readByte(int address);

}
