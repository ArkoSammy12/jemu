package io.github.arkosammy12.jemu.systems;

public interface Bus {

    int getMemorySize();

    int getMemoryBoundsMask();

    int getByte(int address);

    default int getMaximumAddress() {
        return this.getMemoryBoundsMask();
    }

}
