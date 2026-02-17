package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.SystemHost;

public interface CosmacVipHost extends SystemHost {

    Chip8Interpreter getChip8Interpreter();

    enum Chip8Interpreter {
        CHIP_8,
        CHIP_8X,
        NONE
    }

}
