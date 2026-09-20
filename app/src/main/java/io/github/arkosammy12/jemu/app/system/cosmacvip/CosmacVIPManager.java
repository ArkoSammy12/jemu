package io.github.arkosammy12.jemu.app.system.cosmacvip;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.chip8.Chip8Manager;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPKeypad;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class CosmacVIPManager extends SystemManager {

    private final CosmacVIPHost.Chip8Interpreter chip8Interpreter;

    public CosmacVIPManager(Jemu jemu, SystemRegistry systemRegistry, CosmacVIPHost.Chip8Interpreter chip8Interpreter) {
        super(jemu, systemRegistry);
        this.chip8Interpreter = chip8Interpreter;

        this.keyActionMap.put(KeyAction.X, CosmacVIPKeypad.Actions.KEY_0);
        this.keyActionMap.put(KeyAction.NUM_1, CosmacVIPKeypad.Actions.KEY_1);
        this.keyActionMap.put(KeyAction.NUM_2, CosmacVIPKeypad.Actions.KEY_2);
        this.keyActionMap.put(KeyAction.NUM_3, CosmacVIPKeypad.Actions.KEY_3);
        this.keyActionMap.put(KeyAction.Q, CosmacVIPKeypad.Actions.KEY_4);
        this.keyActionMap.put(KeyAction.W, CosmacVIPKeypad.Actions.KEY_5);
        this.keyActionMap.put(KeyAction.E, CosmacVIPKeypad.Actions.KEY_6);
        this.keyActionMap.put(KeyAction.A, CosmacVIPKeypad.Actions.KEY_7);
        this.keyActionMap.put(KeyAction.S, CosmacVIPKeypad.Actions.KEY_8);
        this.keyActionMap.put(KeyAction.D, CosmacVIPKeypad.Actions.KEY_9);
        this.keyActionMap.put(KeyAction.Z, CosmacVIPKeypad.Actions.KEY_A);
        this.keyActionMap.put(KeyAction.C, CosmacVIPKeypad.Actions.KEY_B);
        this.keyActionMap.put(KeyAction.NUM_4, CosmacVIPKeypad.Actions.KEY_C);
        this.keyActionMap.put(KeyAction.R, CosmacVIPKeypad.Actions.KEY_D);
        this.keyActionMap.put(KeyAction.F, CosmacVIPKeypad.Actions.KEY_E);
        this.keyActionMap.put(KeyAction.V, CosmacVIPKeypad.Actions.KEY_F);
    }

    @Override
    public String getName() {
        return switch (this.chip8Interpreter) {
            case NONE -> "COSMAC-VIP";
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
    public Optional<Category> getCategory() {
        return switch (this.chip8Interpreter) {
            case CHIP_8, CHIP_8X -> Optional.of(Chip8Manager.CATEGORY);
            default -> Optional.empty();
        };
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        return new CosmacVIPAdapter(jemu, this, this.chip8Interpreter);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof CosmacVIPAdapter cosmacVIPAdapter && this.chip8Interpreter == cosmacVIPAdapter.getChip8Interpreter();
    }

}
