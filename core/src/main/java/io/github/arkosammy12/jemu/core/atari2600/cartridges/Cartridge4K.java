package io.github.arkosammy12.jemu.core.atari2600.cartridges;

import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;

public class Cartridge4K<E extends Atari2600Emulator> extends Atari2600Cartridge<E> {

    public Cartridge4K(E emulator) {
        super(emulator);
    }

    @Override
    protected int mapROMAddress(int address) {
        return address & 0xFFF;
    }

}