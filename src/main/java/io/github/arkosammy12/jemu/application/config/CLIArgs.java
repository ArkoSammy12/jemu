package io.github.arkosammy12.jemu.application.config;

import io.github.arkosammy12.jemu.application.Main;
import io.github.arkosammy12.jemu.application.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.config.settings.EmulatorSettings;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.application.util.KeyboardLayout;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Optional;

@CommandLine.Command(
        name = "jemu",
        mixinStandardHelpOptions = true,
        version = Main.VERSION_STRING,
        description = "Initializes jemu with the desired configurations and starts emulation."
)
public class CLIArgs implements EmulatorInitializer {

    @CommandLine.Option(
            names = {"--rom", "-r"},
            required = true,
            description = "The path of the file containing the raw binary ROM data."
    )
    private Path romPath;

    @CommandLine.Option(
            names = {"--system", "-v"},
            converter = System.Converter.class,
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "Select the desired system or leave unspecified."
    )
    private Optional<System> system;
    
    @CommandLine.Option(
            names = {"--keyboard-layout", "-k"},
            converter = KeyboardLayout.Converter.class,
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "Select the desired keyboard layout configuration for using the CHIP-8 keypad."
    )
    private Optional<KeyboardLayout> keyboardLayout;


    @Override
    public Optional<byte[]> getRawRom() {
        return Optional.of(EmulatorSettings.readRawRom(romPath.toAbsolutePath()));
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.of(romPath.toAbsolutePath());
    }

    @Override
    public Optional<KeyboardLayout> getKeyboardLayout() {
        return this.keyboardLayout;
    }

    @Override
    public Optional<System> getSystem() {
        return this.system;
    }

}
