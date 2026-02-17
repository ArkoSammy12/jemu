package io.github.arkosammy12.jemu.backend.common;

public interface Bus {

    int readByte(int address);

    void writeByte(int address, int value);

}
