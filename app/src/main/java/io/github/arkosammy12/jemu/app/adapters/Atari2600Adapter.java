package io.github.arkosammy12.jemu.app.adapters;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.atari.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemController;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.LineUnavailableException;
import java.util.Optional;

import static io.github.arkosammy12.jemu.app.util.System.ATARI_2600;

public class Atari2600Adapter extends AbstractSystemAdapter {

    private String romTitle;
    private System system;

    public Atari2600Adapter(Jemu jemu, EmulatorInitializer initializer) throws LineUnavailableException {
        super(jemu, initializer);
    }

    @Override
    protected Emulator createEmulator() {
        return new Atari2600Emulator(this);
    }

    @Override
    @Nullable
    protected SystemController.Action getActionForKeyCode(int keyCode) {
        return null;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    @Override
    public String getSystemName() {
        return this.system.getName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    protected void initialize(Jemu jemu, EmulatorInitializer initializer, boolean tryReset) throws LineUnavailableException {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(ATARI_2600);
        super.initialize(jemu, initializer, tryReset);
    }

}
