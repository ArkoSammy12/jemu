package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.core.chip8.Chip8Host;

import java.util.function.ToIntFunction;

public record Chip8QuirkSet(
        boolean doVFReset,
        Chip8Host.MemoryIncrementQuirk memoryIncrementQuirk,
        boolean doDisplayWait,
        boolean doClipping,
        boolean doShiftVXInPlace,
        boolean doJumpWithVX,
        ToIntFunction<Boolean> instructionsPerFrameSupplier
) {

}
