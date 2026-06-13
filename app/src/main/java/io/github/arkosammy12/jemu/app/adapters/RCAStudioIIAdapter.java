package io.github.arkosammy12.jemu.app.adapters;

import de.gurkenlabs.input4j.InputComponent;
import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import io.github.arkosammy12.jemu.core.rca.studioii.RCAStudioIIEmulator;
import io.github.arkosammy12.jemu.core.rca.studioii.RCAStudioIIKeypad;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.util.Optional;

import static io.github.arkosammy12.jemu.app.util.System.RCA_STUDIO_II;

public class RCAStudioIIAdapter extends AbstractSystemAdapter {

    private String romTitle;
    private final System system;

    public RCAStudioIIAdapter(Jemu jemu, EmulatorInitializer initializer) {
        super(jemu, initializer);
        this.system = initializer.getSystem().orElse(RCA_STUDIO_II);
    }

    @Override
    protected Emulator createEmulator() {
        return new RCAStudioIIEmulator(this);
    }

    @Override
    protected @Nullable RCAStudioIIKeypad.Action getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_ALT -> RCAStudioIIKeypad.Actions.KEYPAD1_0;
            case KeyEvent.VK_Q -> RCAStudioIIKeypad.Actions.KEYPAD1_1;
            case KeyEvent.VK_W -> RCAStudioIIKeypad.Actions.KEYPAD1_2;
            case KeyEvent.VK_E -> RCAStudioIIKeypad.Actions.KEYPAD1_3;
            case KeyEvent.VK_A -> RCAStudioIIKeypad.Actions.KEYPAD1_4;
            case KeyEvent.VK_S -> RCAStudioIIKeypad.Actions.KEYPAD1_5;
            case KeyEvent.VK_D -> RCAStudioIIKeypad.Actions.KEYPAD1_6;
            case KeyEvent.VK_Z -> RCAStudioIIKeypad.Actions.KEYPAD1_7;
            case KeyEvent.VK_X -> RCAStudioIIKeypad.Actions.KEYPAD1_8;
            case KeyEvent.VK_C -> RCAStudioIIKeypad.Actions.KEYPAD1_9;

            case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> RCAStudioIIKeypad.Actions.KEYPAD2_0;
            case KeyEvent.VK_1, KeyEvent.VK_NUMPAD7 -> RCAStudioIIKeypad.Actions.KEYPAD2_1;
            case KeyEvent.VK_2, KeyEvent.VK_NUMPAD8 -> RCAStudioIIKeypad.Actions.KEYPAD2_2;
            case KeyEvent.VK_3, KeyEvent.VK_NUMPAD9 -> RCAStudioIIKeypad.Actions.KEYPAD2_3;
            case KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 -> RCAStudioIIKeypad.Actions.KEYPAD2_4;
            case KeyEvent.VK_5, KeyEvent.VK_NUMPAD5 -> RCAStudioIIKeypad.Actions.KEYPAD2_5;
            case KeyEvent.VK_6, KeyEvent.VK_NUMPAD6 -> RCAStudioIIKeypad.Actions.KEYPAD2_6;
            case KeyEvent.VK_7, KeyEvent.VK_NUMPAD1 -> RCAStudioIIKeypad.Actions.KEYPAD2_7;
            case KeyEvent.VK_8, KeyEvent.VK_NUMPAD2 -> RCAStudioIIKeypad.Actions.KEYPAD2_8;
            case KeyEvent.VK_9, KeyEvent.VK_NUMPAD3 -> RCAStudioIIKeypad.Actions.KEYPAD2_9;
            default -> null;
        };
    }

    @Override
    public @Nullable SystemController.Action getActionForJoypadEvent(InputComponent.ID id) {
        return null;
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
    protected void initialize(Jemu jemu, EmulatorInitializer initializer, boolean tryReset) {
        super.initialize(jemu, initializer, tryReset);
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
    }

}
