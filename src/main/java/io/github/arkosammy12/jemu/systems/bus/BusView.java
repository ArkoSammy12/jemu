package io.github.arkosammy12.jemu.systems.bus;

public interface BusView {

    int getMemorySize();

    int getMemoryBoundsMask();

    int getByte(int address);

    default int getMaximumAddress() {
        return this.getMemoryBoundsMask();
    }

}
