package io.github.arkosammy12.jemu.core.chip8.bus;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;

public class MegaChipBus extends Chip8Bus {

    private static final int MEMORY_BOUNDS_MASK = 0xFFFFFF;
    private static final int MEMORY_SIZE = MEMORY_BOUNDS_MASK + 1;

    public MegaChipBus(Chip8Emulator emulator) {
        super(emulator);
    }

    @Override
    public int getMemorySize() {
        return MEMORY_SIZE;
    }

    @Override
    public int getMemoryBoundsMask() {
        return MEMORY_BOUNDS_MASK;
    }

}
