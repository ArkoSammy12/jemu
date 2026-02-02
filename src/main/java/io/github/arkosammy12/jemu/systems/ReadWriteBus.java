package io.github.arkosammy12.jemu.systems;

public interface ReadWriteBus extends Bus {

    void writeByte(int address, int value);

    int readByte(int address);

}
