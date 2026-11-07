package io.github.arkosammy12.jemu.app.system.atari2600;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Controller;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600SystemHost;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class Atari2600Adapter extends SystemAdapter implements Atari2600SystemHost {

    private final Atari2600Manager atari2600Manager;

    private String romTitle;

    public Atari2600Adapter(Jemu jemu, Atari2600Manager systemManager) throws LineUnavailableException {
        this.atari2600Manager = systemManager;
        super(jemu, systemManager);
    }

    @Override
    protected Emulator createEmulator() {
        return new Atari2600Emulator(this);
    }

    @Override
    public Optional<String> getProgramTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public Optional<CartridgeInfo> getCartridgeInfo() {
        return Optional.ofNullable(this.rom).flatMap(this.atari2600Manager::getDatabaseEntryForRom);
    }

    @Override
    public Optional<Atari2600Emulator.TVFormat> getTVFormatOverride() {
        return this.atari2600Manager.getEmulationSettings().getTVFormatOverride().getHostTVFormat();
    }

    @Override
    public Optional<Atari2600Cartridge.Type> getCartridgeTypeOverride() {
        return this.atari2600Manager.getEmulationSettings().getCartridgeTypeOverride().getHostCartridgeType();
    }

    @Override
    public boolean getColorSwitch() {
        return switch (this.atari2600Manager.getEmulationSettings().getTVType()) {
            case COLOR -> true;
            case BLACK_AND_WHITE -> false;
        };
    }

    @Override
    public boolean getLeftDifficulty() {
        return switch (this.atari2600Manager.getEmulationSettings().getLeftDifficulty()) {
            case ADVANCED -> true;
            case BEGINNER -> false;
        };
    }

    @Override
    public boolean getRightDifficulty() {
        return switch (this.atari2600Manager.getEmulationSettings().getRightDifficulty()) {
            case ADVANCED -> true;
            case BEGINNER -> false;
        };
    }

    @Override
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
