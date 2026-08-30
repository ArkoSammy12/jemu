package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.commodore64.cartridges.GenericCartridge;
import io.github.arkosammy12.jemu.core.commodore64.cartridges.MagicDeskCartridge;
import io.github.arkosammy12.jemu.core.commodore64.crt.CRTFile;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

public abstract class Commodore64Cartridge<E extends Commodore64Emulator> implements ExpansionPortDevice {

    protected final E emulator;

    protected Commodore64Cartridge(E emulator, CRTFile crtFile) {
        this.emulator = emulator;
    }

    @Override
    public int read(int address, Commodore64Bus.AddressRegion addressRegion) {
        return switch (addressRegion) {
            case ROML -> this.readROML(address);
            case ROMH -> this.readROMH(address);
            case IO1 -> this.readIO1(address);
            case IO2 -> this.readIO2(address);
            default -> this.emulator.getBus().combineWithDataBus(0x00, 0x00);
        };
    }

    @Override
    public void write(int address, int value, Commodore64Bus.AddressRegion addressRegion) {
        switch (addressRegion) {
            case ROML -> this.writeROML(address, value);
            case ROMH -> this.writeROMH(address, value);
            case IO1 -> this.writeIO1(address, value);
            case IO2 -> this.writeIO2(address, value);
        }
    }

    protected abstract int readROML(int address);

    protected void writeROML(int address, int value) {

    }

    protected int readROMH(int address) {
        return this.emulator.getBus().combineWithDataBus(0x00, 0x00);
    }

    protected void writeROMH(int address, int value) {

    }

    protected int readIO1(int address) {
        return this.emulator.getBus().combineWithDataBus(0, 0x00);
    }

    protected void writeIO1(int address, int value) {

    }

    protected int readIO2(int address) {
        return this.emulator.getBus().combineWithDataBus(0, 0x00);
    }

    protected void writeIO2(int address, int value) {

    }

    public static <E extends Commodore64Emulator> Commodore64Cartridge<E> getCartridge(E emulator, CRTFile crtFile) {
        int cartridgeType = crtFile.getCartridgeHardwareType();
        return switch (cartridgeType) {
            case 0 -> new GenericCartridge<>(emulator, crtFile);
            case 19 -> new MagicDeskCartridge<>(emulator, crtFile);
            default -> throw new ROMInitializationException("Unsupported .CRT cartridge hardware type %d!".formatted(cartridgeType));
        };
    }

}
