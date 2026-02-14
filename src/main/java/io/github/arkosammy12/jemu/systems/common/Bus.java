package io.github.arkosammy12.jemu.systems.common;

public interface Bus {

    int readByte(int address);

    void writeByte(int address, int value);

}
