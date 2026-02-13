package io.github.arkosammy12.jemu.config.settings;

import io.github.arkosammy12.jemu.config.initializers.CommonInitializer;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.common.Emulator;
import io.github.arkosammy12.jemu.systems.gameboy.GameBoyEmulator;
import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.System;

import java.util.Optional;

public class GameBoyEmulatorSettings extends AbstractEmulatorSettings {

    private final String romTitle;

    private final DisplayAngle displayAngle;
    private final System system;
    private final Model model;

    public GameBoyEmulatorSettings(Jemu jemu, CommonInitializer initializer, Model model) {
        super(jemu, initializer);
        this.displayAngle = initializer.getDisplayAngle().orElse(DisplayAngle.DEG_0);
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(System.GAME_BOY);
        this.model = model;
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public DisplayAngle getDisplayAngle() {
        return this.displayAngle;
    }

    @Override
    public System getSystem() {
        return this.system;
    }

    public Model getModel() {
        return this.model;
    }

    @Override
    public Emulator getEmulator() {
        return new GameBoyEmulator(this);
    }

    public enum Model {
        DMG,
        CGB,
    }

}
