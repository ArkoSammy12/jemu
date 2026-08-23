package io.github.arkosammy12.jemu.app.system.rcastudioii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.studioii.RCAStudioIIEmulator;

import javax.sound.sampled.LineUnavailableException;
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
    protected void initialize(EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        super.initialize(initializer, tryReset);
    }

}
