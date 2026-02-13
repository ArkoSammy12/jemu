package io.github.arkosammy12.jemu.config.settings;

import io.github.arkosammy12.jemu.config.initializers.CommonInitializer;
import io.github.arkosammy12.jemu.systems.cosmacvip.CosmacVipEmulator;
import io.github.arkosammy12.jemu.systems.common.Emulator;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.util.DisplayAngle;
import io.github.arkosammy12.jemu.util.System;

import java.util.Optional;

import static io.github.arkosammy12.jemu.util.System.COSMAC_VIP;

public class CosmacVipEmulatorSettings extends AbstractEmulatorSettings {

    private final String romTitle;

    private final DisplayAngle displayAngle;
    private final System system;
    private final Chip8Interpreter chip8Interpreter;

    public CosmacVipEmulatorSettings(Jemu jemu, CommonInitializer initializer, Chip8Interpreter chip8Interpreter) {
        super(jemu, initializer);

        this.displayAngle = initializer.getDisplayAngle().orElse(DisplayAngle.DEG_0);
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(COSMAC_VIP);
        this.chip8Interpreter = chip8Interpreter;
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

    @Override
    public Emulator getEmulator() {
        return new CosmacVipEmulator(this, this.chip8Interpreter);
    }

    public enum Chip8Interpreter {
        CHIP_8,
        CHIP_8X,
        NONE
    }

}
