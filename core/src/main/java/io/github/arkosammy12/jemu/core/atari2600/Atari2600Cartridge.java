package io.github.arkosammy12.jemu.core.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.MissingROMException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.ines.INESFile.KB_4;

public class Atari2600Cartridge<E extends Atari2600Emulator> implements Bus {

    private final byte[] rom;

    private Atari2600Cartridge(E emulator) {
        Optional<byte[]> rom = emulator.getHost().getRom();
        if (rom.isEmpty()) {
            throw new MissingROMException(emulator.getHost().getSystemName());
        }
        this.rom = rom.get();
        if (this.rom.length > KB_4) {
            throw new ROMInitializationException("ROM cartridges bigger than 4KB are not yet supported!");
        }
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
