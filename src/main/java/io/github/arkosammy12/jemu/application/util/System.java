package io.github.arkosammy12.jemu.application.util;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.adapters.DefaultCosmacVIPAdapter;
import io.github.arkosammy12.jemu.application.adapters.DefaultGameBoyAdapter;
import io.github.arkosammy12.jemu.application.adapters.DefaultSystemAdapter;
import io.github.arkosammy12.jemu.application.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.application.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.backend.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.application.io.Serializable;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.backend.gameboy.GameBoyHost;
import it.unimi.dsi.fastutil.Pair;
import picocli.CommandLine;

import java.util.Optional;
import java.util.function.Function;

public enum System implements DisplayNameProvider, Serializable {
    COSMAC_VIP("cosmac-vip", "COSMAC-VIP", args -> new DefaultCosmacVIPAdapter(args.jemu(), args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.NONE)),
    VIP_CHIP_8("vip-chip-8", "VIP CHIP-8", args -> new DefaultCosmacVIPAdapter(args.jemu(), args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8)),
    VIP_CHIP_8X("vip-chip-8x", "VIP CHIP-8X", args -> new DefaultCosmacVIPAdapter(args.jemu(), args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8X)),
    GAME_BOY("game-boy", "Game Boy", args -> new DefaultGameBoyAdapter(args.jemu(), args.coreInitializer(), GameBoyHost.Model.DMG));

    private final String identifier;
    private final String displayName;
    private final Function<EmulatorSettingsArgs, ? extends DefaultSystemAdapter> args;

    System(String identifier, String displayName, Function<EmulatorSettingsArgs, ? extends DefaultSystemAdapter> args) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.args = args;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public static DefaultSystemAdapter getSystemAdapter(Jemu jemu, CoreInitializer initializer) {
        Optional<System> optionalVariant = initializer.getSystem();
        if (optionalVariant.isPresent()) {
             return optionalVariant.get().args.apply(new EmulatorSettingsArgs(jemu, initializer));
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

    private record EmulatorSettingsArgs(Jemu jemu, CoreInitializer coreInitializer) {}

}
