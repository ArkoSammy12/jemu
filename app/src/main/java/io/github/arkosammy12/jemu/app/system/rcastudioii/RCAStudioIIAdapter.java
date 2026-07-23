package io.github.arkosammy12.jemu.app.system.rcastudioii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.studioii.RCAStudioIIEmulator;
import io.github.arkosammy12.jemu.core.studioii.RCAStudioIIKeypad;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class RCAStudioIIAdapter extends SystemAdapter {

    private String romTitle;

    public RCAStudioIIAdapter(Jemu jemu, SystemManager systemManager) throws LineUnavailableException {
        super(jemu, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new RCAStudioIIEmulator(this);
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    protected @Nullable RCAStudioIIKeypad.Action getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_1 -> RCAStudioIIKeypad.Actions.KEYPADA_1;
            case KeyEvent.VK_2 -> RCAStudioIIKeypad.Actions.KEYPADA_2;
            case KeyEvent.VK_3 -> RCAStudioIIKeypad.Actions.KEYPADA_3;
            case KeyEvent.VK_Q -> RCAStudioIIKeypad.Actions.KEYPADA_4;
            case KeyEvent.VK_W -> RCAStudioIIKeypad.Actions.KEYPADA_5;
            case KeyEvent.VK_E -> RCAStudioIIKeypad.Actions.KEYPADA_6;
            case KeyEvent.VK_A -> RCAStudioIIKeypad.Actions.KEYPADA_7;
            case KeyEvent.VK_S -> RCAStudioIIKeypad.Actions.KEYPADA_8;
            case KeyEvent.VK_D -> RCAStudioIIKeypad.Actions.KEYPADA_9;
            case KeyEvent.VK_X -> RCAStudioIIKeypad.Actions.KEYPADA_0;

            case KeyEvent.VK_7, KeyEvent.VK_NUMPAD7 -> RCAStudioIIKeypad.Actions.KEYPADB_1;
            case KeyEvent.VK_8, KeyEvent.VK_NUMPAD8 -> RCAStudioIIKeypad.Actions.KEYPADB_2;
            case KeyEvent.VK_9, KeyEvent.VK_NUMPAD9 -> RCAStudioIIKeypad.Actions.KEYPADB_3;
            case KeyEvent.VK_U, KeyEvent.VK_NUMPAD4 -> RCAStudioIIKeypad.Actions.KEYPADB_4;
            case KeyEvent.VK_I, KeyEvent.VK_NUMPAD5 -> RCAStudioIIKeypad.Actions.KEYPADB_5;
            case KeyEvent.VK_O, KeyEvent.VK_NUMPAD6 -> RCAStudioIIKeypad.Actions.KEYPADB_6;
            case KeyEvent.VK_J, KeyEvent.VK_NUMPAD1 -> RCAStudioIIKeypad.Actions.KEYPADB_7;
            case KeyEvent.VK_K, KeyEvent.VK_NUMPAD2 -> RCAStudioIIKeypad.Actions.KEYPADB_8;
            case KeyEvent.VK_L, KeyEvent.VK_NUMPAD3 -> RCAStudioIIKeypad.Actions.KEYPADB_9;
            case KeyEvent.VK_COMMA, KeyEvent.VK_NUMPAD0 -> RCAStudioIIKeypad.Actions.KEYPADB_0;
            default -> null;
        };
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
