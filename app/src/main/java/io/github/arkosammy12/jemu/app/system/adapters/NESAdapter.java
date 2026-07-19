package io.github.arkosammy12.jemu.app.system.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.managers.NESManager;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.nes.NESController;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.NESHost;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;

public class NESAdapter extends SystemAdapter implements NESHost {

    private final NESManager nesManager;
    private String romTitle;

    public NESAdapter(Jemu jemu, NESManager systemManager) throws LineUnavailableException {
        this.nesManager = systemManager;
        super(jemu, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new NESEmulator(this);
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<Path> getSaveDataDirectory() {
        return this.jemu.getSavesDirectory();
    }

    @Override
    public Optional<CartridgeInfo> getExternalCartridgeInfo(int totalRomSize, boolean hasByteTrainer) {
        return this.nesManager.findDatabaseEntryFromNesFile(this.rom, totalRomSize, hasByteTrainer);
    }

    @Override
    @Nullable
    protected NESController.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W -> NESController.Actions.JOY1_UP;
            case KeyEvent.VK_S -> NESController.Actions.JOY1_DOWN;
            case KeyEvent.VK_A -> NESController.Actions.JOY1_LEFT;
            case KeyEvent.VK_D -> NESController.Actions.JOY1_RIGHT;
            case KeyEvent.VK_ENTER -> NESController.Actions.JOY1_START;
            case KeyEvent.VK_BACK_SPACE -> NESController.Actions.JOY1_SELECT;
            case KeyEvent.VK_J -> NESController.Actions.JOY1_A;
            case KeyEvent.VK_K -> NESController.Actions.JOY1_B;
            default -> null;
        };
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
