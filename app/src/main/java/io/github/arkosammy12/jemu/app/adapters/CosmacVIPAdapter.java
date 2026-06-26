package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.rca.cosmacvip.CosmacVIPKeypad;
import io.github.arkosammy12.jemu.core.rca.cosmacvip.CosmacVIPEmulator;
import io.github.arkosammy12.jemu.core.rca.cosmacvip.CosmacVIPHost;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

import static io.github.arkosammy12.jemu.app.util.System.COSMAC_VIP;

public class CosmacVIPAdapter extends AbstractSystemAdapter implements CosmacVIPHost {

    private final String romTitle;
    private final System system;
    private final Chip8Interpreter chip8Interpreter;

    public CosmacVIPAdapter(Jemu jemu, EmulatorInitializer initializer, Chip8Interpreter chip8Interpreter) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(COSMAC_VIP);
        this.chip8Interpreter = chip8Interpreter;
        super(jemu, initializer);
    }

    @Override
    protected Emulator createEmulator() {
        return new CosmacVIPEmulator(this);
    }

    @Override
    @Nullable
    protected CosmacVIPKeypad.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_X -> CosmacVIPKeypad.Actions.KEY_0;
            case KeyEvent.VK_1 -> CosmacVIPKeypad.Actions.KEY_1;
            case KeyEvent.VK_2 -> CosmacVIPKeypad.Actions.KEY_2;
            case KeyEvent.VK_3 -> CosmacVIPKeypad.Actions.KEY_3;
            case KeyEvent.VK_Q -> CosmacVIPKeypad.Actions.KEY_4;
            case KeyEvent.VK_W -> CosmacVIPKeypad.Actions.KEY_5;
            case KeyEvent.VK_E -> CosmacVIPKeypad.Actions.KEY_6;
            case KeyEvent.VK_A -> CosmacVIPKeypad.Actions.KEY_7;
            case KeyEvent.VK_S -> CosmacVIPKeypad.Actions.KEY_8;
            case KeyEvent.VK_D -> CosmacVIPKeypad.Actions.KEY_9;
            case KeyEvent.VK_Z -> CosmacVIPKeypad.Actions.KEY_A;
            case KeyEvent.VK_C -> CosmacVIPKeypad.Actions.KEY_B;
            case KeyEvent.VK_4 -> CosmacVIPKeypad.Actions.KEY_C;
            case KeyEvent.VK_R -> CosmacVIPKeypad.Actions.KEY_D;
            case KeyEvent.VK_F -> CosmacVIPKeypad.Actions.KEY_E;
            case KeyEvent.VK_V -> CosmacVIPKeypad.Actions.KEY_F;
            default -> null;
        };
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public String getSystemName() {
        return this.system.getDisplayName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
    }

}
