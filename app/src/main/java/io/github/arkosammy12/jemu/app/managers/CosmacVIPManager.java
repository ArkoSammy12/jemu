package io.github.arkosammy12.jemu.app.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.CosmacVIPAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;

import javax.sound.sampled.LineUnavailableException;
import java.util.Optional;

public class CosmacVIPManager implements SystemManager {

    private final CosmacVIPHost.Chip8Interpreter chip8Interpreter;

    public CosmacVIPManager(CosmacVIPHost.Chip8Interpreter chip8Interpreter) {
        this.chip8Interpreter = chip8Interpreter;
    }

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new CosmacVIPAdapter(jemu, system, this.chip8Interpreter, this);
    }

    @Override
    public String getName() {
        return switch (this.chip8Interpreter) {
            case NONE -> "COSMACV-VIP";
            case CHIP_8 -> "VIP CHIP-8";
            case CHIP_8X -> "VIP CHIP-8X";
        };
    }

    @Override
    public String getId() {
        return switch (this.chip8Interpreter) {
            case NONE -> "cosmac-vip";
            case CHIP_8 -> "vip-chip8";
            case CHIP_8X -> "vip-chip8x";
        };
    }

    @Override
    public Optional<String[]> getFileExtensions() {
        return switch (this.chip8Interpreter) {
            case NONE -> Optional.of(new String[] {"cos"});
            case CHIP_8 -> Optional.of(new String[] {"ch8", "hc8"});
            case CHIP_8X -> Optional.of(new String[] {"ch8", "c8x"});
        };
    }

}
