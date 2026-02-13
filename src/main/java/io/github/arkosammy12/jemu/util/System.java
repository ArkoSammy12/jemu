package io.github.arkosammy12.jemu.util;

import io.github.arkosammy12.jemu.config.settings.GameBoyEmulatorSettings;
import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.Serializable;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.settings.CosmacVipEmulatorSettings;
import io.github.arkosammy12.jemu.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.systems.common.Emulator;
import picocli.CommandLine;

import java.util.Optional;
import java.util.function.BiFunction;

public enum System implements DisplayNameProvider, Serializable {
    HYBRID_CHIP_8("hybrid-chip-8", "HYBRID CHIP-8", (jemu, initializer) -> new CosmacVipEmulatorSettings(jemu, initializer, CosmacVipEmulatorSettings.Chip8Interpreter.CHIP_8)),
    HYBRID_CHIP_8X("hybrid-chip-8x", "HYBRID CHIP-8X", (jemu, initializer) -> new CosmacVipEmulatorSettings(jemu, initializer, CosmacVipEmulatorSettings.Chip8Interpreter.CHIP_8X)),
    COSMAC_VIP("cosmac-vip", "COSMAC-VIP", (jemu, initializer) -> new CosmacVipEmulatorSettings(jemu, initializer, CosmacVipEmulatorSettings.Chip8Interpreter.NONE)),
    GAME_BOY("game-boy", "Game Boy", (jemu, initializer) -> new GameBoyEmulatorSettings(jemu, initializer, GameBoyEmulatorSettings.Model.DMG));

    private final String identifier;
    private final String displayName;
    private final BiFunction<Jemu, EmulatorInitializer, ? extends EmulatorSettings> initializer;

    System(String identifier, String displayName, BiFunction<Jemu, EmulatorInitializer, ? extends EmulatorSettings> initializer) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.initializer = initializer;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public static Emulator getEmulator(Jemu jemu, EmulatorInitializer mainInitializer) {
        Optional<System> optionalVariant = mainInitializer.getSystem();
        if (optionalVariant.isPresent()) {
            return optionalVariant.get().initializer.apply(jemu, mainInitializer).getEmulator();
        }
        throw new EmulatorException("Must select a system!");
    }

    public static System getSystemForIdentifier(String identifier) {
        for (System system : System.values()) {
            if (system.identifier.equals(identifier)) {
                return system;
            }
        }
        throw new IllegalArgumentException("Unknown system identifier \"" + identifier + "\"!");
    }

    @Override
    public String getSerializedString() {
        return this.identifier;
    }

    public static class Converter implements CommandLine.ITypeConverter<System> {

        @Override
        public System convert(String value) {
            return getSystemForIdentifier(value);
        }

    }

}
