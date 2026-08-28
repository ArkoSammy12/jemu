package io.github.arkosammy12.jemu.core.commodore64;

public interface ExpansionPortDevice {

    default boolean getEXROM() {
        return false;
    }

    default boolean getGAME() {
        return false;
    }

    default boolean getNMI() {
        return false;
    }

    default boolean getIRQ() {
        return false;
    }

    default boolean getDMA() {
        return false;
    }

    default boolean getRESET() {
        return false;
    }

    int read(int address, Commodore64Bus.AddressRegion addressRegion);

    default int readVIC2(int address) {
        return this.read(address, Commodore64Bus.AddressRegion.ROMH);
    }

    default void write(int address, int value, Commodore64Bus.AddressRegion addressRegion) {

    }

    default void cyclePHI2() {

    }

    default void cycleDot() {

    }

    interface SystemBus {

        boolean getIRQ();

        boolean getBA();

        Commodore64Bus<?> getBus();

    }


}
