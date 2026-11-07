package io.github.arkosammy12.jemu.core.common;

public interface Bus {

    int readByte(int address);

    void writeByte(int address, int value);

}
