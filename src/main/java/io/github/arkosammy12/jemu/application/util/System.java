package io.github.arkosammy12.jemu.application.util;

import io.github.arkosammy12.jemu.application.config.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.application.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.config.settings.CosmacVipEmulatorSettings;
import io.github.arkosammy12.jemu.application.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.application.config.settings.GameBoyEmulatorSettings;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.application.config.Serializable;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import it.unimi.dsi.fastutil.Pair;
import picocli.CommandLine;

import java.util.Optional;
import java.util.function.Function;

public enum System implements DisplayNameProvider, Serializable {
    COSMAC_VIP("cosmac-vip", "COSMAC-VIP", args -> new CosmacVipEmulatorSettings(args.coreInitializer(), CosmacVipEmulatorSettings.Chip8Interpreter.NONE)),
    VIP_CHIP_8("vip-chip-8", "VIP CHIP-8", args -> new CosmacVipEmulatorSettings(args.coreInitializer(), CosmacVipEmulatorSettings.Chip8Interpreter.CHIP_8)),
    VIP_CHIP_8X("vip-chip-8x", "VIP CHIP-8X", args -> new CosmacVipEmulatorSettings(args.coreInitializer(), CosmacVipEmulatorSettings.Chip8Interpreter.CHIP_8X)),
    GAME_BOY("game-boy", "Game Boy", args -> new GameBoyEmulatorSettings(args.coreInitializer(), GameBoyEmulatorSettings.Model.DMG));

    private final String identifier;
    private final String displayName;
    private final Function<EmulatorSettingsArgs, ? extends EmulatorSettings> args;

    System(String identifier, String displayName, Function<EmulatorSettingsArgs, ? extends EmulatorSettings> args) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.args = args;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public static Pair<Emulator, EmulatorSettings> getEmulator(EmulatorInitializer mainInitializer) {
        Optional<System> optionalVariant = mainInitializer.getSystem();
        if (optionalVariant.isPresent()) {
            EmulatorSettings emulatorSettings = optionalVariant.get().args.apply(new EmulatorSettingsArgs(mainInitializer));
            return Pair.of(emulatorSettings.getEmulator(), emulatorSettings);
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

    private record EmulatorSettingsArgs(CoreInitializer coreInitializer) {}

}
