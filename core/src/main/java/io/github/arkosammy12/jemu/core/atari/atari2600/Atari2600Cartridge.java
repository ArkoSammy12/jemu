package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;

import java.util.Optional;

public class Atari2600Cartridge<E extends Atari2600Emulator> implements Bus {

    private final E emulator;
    private final byte[] rom;

    private Atari2600Cartridge(E emulator) {
        this.emulator = emulator;
        Optional<byte[]> rom = this.emulator.getHost().getRom();
        if (rom.isEmpty()) {
            throw new MissingROMException(this.emulator.getHost().getSystemName());
        }
        this.rom = rom.get();
    }

    @Override
    public int readByte(int address) {
        return this.rom[(address & 0xFFF) % this.rom.length];
    }

    @Override
    public void writeByte(int address, int value) {

    }

    public static <E extends Atari2600Emulator> Atari2600Cartridge<E> getCartridge(E emulator) {
        return new Atari2600Cartridge<>(emulator);
    }

}
