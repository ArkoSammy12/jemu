package io.github.arkosammy12.jemu.app.system.nes;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.NESHost;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;

import javax.sound.sampled.LineUnavailableException;
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
    public Optional<String> getProgramTitle() {
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
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
