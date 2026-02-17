package io.github.arkosammy12.jemu.application.config.settings;

import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVipHost;
import io.github.arkosammy12.jemu.application.config.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVipEmulator;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.application.util.System;

import java.util.Optional;

import static io.github.arkosammy12.jemu.application.util.System.COSMAC_VIP;

public class CosmacVipEmulatorSettings extends AbstractEmulatorSettings implements CosmacVipHost {

    private final String romTitle;

    private final System system;
    private final Chip8Interpreter chip8Interpreter;

    public CosmacVipEmulatorSettings(CoreInitializer initializer, Chip8Interpreter chip8Interpreter) {
        super(initializer);

        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = initializer.getSystem().orElse(COSMAC_VIP);
        this.chip8Interpreter = chip8Interpreter;
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
    public System getSystem() {
        return this.system;
    }

    @Override
    public Emulator getEmulator() {
        return new CosmacVipEmulator(this);
    }

    @Override
    public CosmacVipHost.Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
    }

}
