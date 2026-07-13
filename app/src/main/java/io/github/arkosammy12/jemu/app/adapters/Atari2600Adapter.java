package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.managers.Atari2600Manager;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Controller;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600SystemHost;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class Atari2600Adapter extends SystemAdapter implements Atari2600SystemHost {

    private final Atari2600Manager atari2600Manager;

    private String romTitle;

    public Atari2600Adapter(Jemu jemu, System system, Atari2600Manager systemManager) throws LineUnavailableException {
        this.atari2600Manager = systemManager;
        super(jemu, system, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new Atari2600Emulator(this);
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<CartridgeInfo> getCartridgeInfo() {
        return Optional.ofNullable(this.rom).flatMap(this.atari2600Manager::getDatabaseEntryForRom);
    }

    @Override
    @Nullable
    protected SystemController.Action getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_E -> Atari2600Controller.Actions.GAME_SELECT;
            case KeyEvent.VK_R -> Atari2600Controller.Actions.GAME_RESET;

            case KeyEvent.VK_W -> Atari2600Controller.Actions.JOYSTICK0_UP;
            case KeyEvent.VK_S -> Atari2600Controller.Actions.JOYSTICK0_DOWN;
            case KeyEvent.VK_A -> Atari2600Controller.Actions.JOYSTICK0_LEFT;
            case KeyEvent.VK_D -> Atari2600Controller.Actions.JOYSTICK0_RIGHT;
            case KeyEvent.VK_F -> Atari2600Controller.Actions.JOYSTICK0_FIRE;


            case KeyEvent.VK_I -> Atari2600Controller.Actions.JOYSTICK1_UP;
            case KeyEvent.VK_K -> Atari2600Controller.Actions.JOYSTICK1_DOWN;
            case KeyEvent.VK_J -> Atari2600Controller.Actions.JOYSTICK1_LEFT;
            case KeyEvent.VK_L -> Atari2600Controller.Actions.JOYSTICK1_RIGHT;
            case KeyEvent.VK_SEMICOLON -> Atari2600Controller.Actions.JOYSTICK1_FIRE;
            default -> null;
        };
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
