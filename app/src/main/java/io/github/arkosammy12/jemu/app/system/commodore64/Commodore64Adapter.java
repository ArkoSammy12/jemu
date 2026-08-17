package io.github.arkosammy12.jemu.app.system.commodore64;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Host;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.nio.file.Path;
import java.util.Optional;

public class Commodore64Adapter extends SystemAdapter implements Commodore64Host {

    private String romTitle;
    private final Commodore64Manager commodore64Manager;

    public Commodore64Adapter(Jemu jemu, Commodore64Manager systemManager) throws LineUnavailableException {
        super(jemu, systemManager);
        this.commodore64Manager = systemManager;
    }

    @Override
    protected Emulator createEmulator() {
        return new Commodore64Emulator(this);
    }

    @Override
    protected @Nullable SystemController.Action getActionForKeyCode(int keyCode) {
        return null;
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<Path> getKernalROMPath() {
        return commodore64Manager.getEmulationSettings().getKernalRomPath();
    }

    @Override
    public Optional<Path> getBASICRomPath() {
        return commodore64Manager.getEmulationSettings().getBasicRomPath();
    }

    @Override
    public Optional<Path> getCharacterROMPath() {
        return commodore64Manager.getEmulationSettings().getCharacterRomPath();
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
