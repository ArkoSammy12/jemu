package io.github.arkosammy12.jemu.core.chip8.bus;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;

public class Chip8XBus extends Chip8Bus {

    private static final int PROGRAM_START = 0x300;

    public Chip8XBus(Chip8Emulator emulator) {
        super(emulator);
    }

    @Override
    public int getProgramStart() {
        return PROGRAM_START;
    }

}
