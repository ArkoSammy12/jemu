package io.github.arkosammy12.jemu.app.system.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.adapters.CosmacVIPAdapter;
import io.github.arkosammy12.jemu.app.system.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;

public class CosmacVIPManager extends SystemManager {

    private final CosmacVIPHost.Chip8Interpreter chip8Interpreter;

    public CosmacVIPManager(Jemu jemu, SystemRegistry systemRegistry, CosmacVIPHost.Chip8Interpreter chip8Interpreter) {
        super(jemu, systemRegistry);
        this.chip8Interpreter = chip8Interpreter;
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
    public Collection<String> getFileExtensions() {
        return switch (this.chip8Interpreter) {
            case NONE -> List.of("cos");
            case CHIP_8 -> List.of("ch8", "hc8");
            case CHIP_8X -> List.of("ch8", "c8x");
        };
    }

    @Override
    public SystemAdapter createSystem() throws LineUnavailableException {
        return new CosmacVIPAdapter(jemu, this.chip8Interpreter, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof CosmacVIPAdapter cosmacVIPAdapter && this.chip8Interpreter == cosmacVIPAdapter.getChip8Interpreter();
    }

}
