package io.github.arkosammy12.jemu.core.cosmacvip;

import io.github.arkosammy12.jemu.core.common.SystemHost;

public interface CosmacVIPHost extends SystemHost {

    Chip8Interpreter getChip8Interpreter();

    enum Chip8Interpreter {
        CHIP_8,
        CHIP_8X,
        NONE
    }

}
