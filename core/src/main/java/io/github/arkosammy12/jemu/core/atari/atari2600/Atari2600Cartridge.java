package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.Bus;

public class Atari2600Cartridge<E extends Atari2600Emulator> implements Bus {

    private final E emulator;

    private Atari2600Cartridge(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        return 0;
    }

    @Override
    public void writeByte(int address, int value) {

    }

    public static <E extends Atari2600Emulator> Atari2600Cartridge<E> getCartridge(E emulator) {
        return new Atari2600Cartridge<>(emulator);
    }

}
