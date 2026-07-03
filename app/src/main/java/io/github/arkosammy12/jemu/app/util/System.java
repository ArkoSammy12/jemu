package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.*;
import io.github.arkosammy12.jemu.app.io.EmulatorInitializer;
import io.github.arkosammy12.jemu.core.rca.cosmacvip.CosmacVIPHost;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyHost;
import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import picocli.CommandLine;

import java.util.Optional;

public enum System implements DisplayNameProvider, SystemDescriptor {
    COSMAC_VIP("cosmac-vip", "COSMAC-VIP", new String[] {"cos", "bin"}, args -> new CosmacVIPAdapter(args.jemu(), args.emulatorInitializer(), CosmacVIPHost.Chip8Interpreter.NONE)),
    VIP_CHIP_8("vip-chip8", "VIP CHIP-8", new String[] {"ch8", "hc8"}, args -> new CosmacVIPAdapter(args.jemu(), args.emulatorInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8)),
    VIP_CHIP_8X("vip-chip8x", "VIP CHIP-8X", new String[] {"ch8", "c8x"}, args -> new CosmacVIPAdapter(args.jemu(), args.emulatorInitializer(), CosmacVIPHost.Chip8Interpreter.CHIP_8X)),
    RCA_STUDIO_II("rca-studioii", "RCA Studio II", new String[]{"bin", "st2"}, args -> new RCAStudioIIAdapter(args.jemu(), args.emulatorInitializer())),
    GAME_BOY("gameboy", "Game Boy", new String[] {"gb"}, args -> new GameBoyAdapter(args.jemu(), args.emulatorInitializer(), GameBoyHost.Model.DMG)),
    GAME_BOY_COLOR("gameboy-color", "Game Boy Color", new String[] {"gbc"}, args -> new GameBoyAdapter(args.jemu(), args.emulatorInitializer(), GameBoyHost.Model.CGB)),
    NES("nes", "Nintendo Entertainment System", new String[] {"nes"}, args -> new NESAdapter(args.jemu(), args.emulatorInitializer()));

    private final String identifier;
    private final String displayName;
    private final String[] fileExtensions;
    private final ThrowingFunction<EmulatorSettingsArgs, ? extends AbstractSystemAdapter> args;

    System(String identifier, String displayName, String[] fileExtensions, ThrowingFunction<EmulatorSettingsArgs, ? extends AbstractSystemAdapter> args) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.fileExtensions = fileExtensions;
        this.args = args;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    public static AbstractSystemAdapter getSystemAdapter(Jemu jemu, EmulatorInitializer initializer) throws Exception {
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
    public String getName() {
        return this.getDisplayName();
    }

    @Override
    public String getId() {
        return this.identifier;
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

    private record EmulatorSettingsArgs(Jemu jemu, EmulatorInitializer emulatorInitializer) {}

    private interface ThrowingFunction<T, R> {

        R apply(T t) throws Exception;

    }

}
