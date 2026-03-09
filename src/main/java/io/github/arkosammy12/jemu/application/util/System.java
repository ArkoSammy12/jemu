package io.github.arkosammy12.jemu.application.util;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.adapters.DefaultCosmacVIPAdapter;
import io.github.arkosammy12.jemu.application.adapters.DefaultGameBoyAdapter;
import io.github.arkosammy12.jemu.application.adapters.DefaultSystemAdapter;
import io.github.arkosammy12.jemu.application.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.core.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.application.io.Serializable;
import io.github.arkosammy12.jemu.core.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import picocli.CommandLine;

import java.util.Optional;
import java.util.function.Function;

public enum System implements DisplayNameProvider, Serializable, SystemDescriptor {
    COSMAC_VIP("cosmac-vip", "COSMAC-VIP", new String[] {"cos", "bin"}, args -> new DefaultCosmacVIPAdapter(args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.NONE)),
    VIP_CHIP_8("vip-chip-8", "VIP CHIP-8", new String[] {"ch8", "hc8"}, args -> new DefaultCosmacVIPAdapter(args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8)),
    VIP_CHIP_8X("vip-chip-8x", "VIP CHIP-8X", new String[] {"ch8", "c8x"}, args -> new DefaultCosmacVIPAdapter(args.coreInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8X)),
    GAME_BOY("game-boy", "Game Boy", new String[] {"gb"}, args -> new DefaultGameBoyAdapter(args.coreInitializer(), GameBoyHost.Model.DMG));

    private final String identifier;
    private final String displayName;
    private final String[] fileExtensions;
    private final Function<EmulatorSettingsArgs, ? extends DefaultSystemAdapter> args;

    System(String identifier, String displayName, String[] fileExtensions, Function<EmulatorSettingsArgs, ? extends DefaultSystemAdapter> args) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.fileExtensions = fileExtensions;
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

    @Override
    public String getName() {
        return this.getDisplayName();
    }

    @Override
    public Optional<String[]> getFileExtensions() {
        return Optional.ofNullable(this.fileExtensions);
    }

    public static class Converter implements CommandLine.ITypeConverter<System> {

        @Override
        public System convert(String value) {
            return getSystemForIdentifier(value);
        }

    }

    private record EmulatorSettingsArgs(Jemu jemu, CoreInitializer coreInitializer) {}

}
