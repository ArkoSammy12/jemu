package io.github.arkosammy12.jemu.app.io;

import io.github.arkosammy12.jemu.app.Main;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.app.util.System;
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

    private final int quickExitCode;

    public CLIArgs(String[] args) {
        CommandLine cli = new CommandLine(this);
        CommandLine.ParseResult parseResult = cli.parseArgs(args);
        Integer executeHelpResult = CommandLine.executeHelpRequest(parseResult);
        int exitCodeOnUsageHelp = cli.getCommandSpec().exitCodeOnUsageHelp();
        int exitCodeOnVersionHelp = cli.getCommandSpec().exitCodeOnVersionHelp();
        if (executeHelpResult != null) {
            if (executeHelpResult == exitCodeOnUsageHelp) {
                this.quickExitCode = exitCodeOnUsageHelp;
            } else if (executeHelpResult == exitCodeOnVersionHelp) {
                this.quickExitCode = exitCodeOnVersionHelp;
            } else {
                this.quickExitCode = -1;
            }
        } else {
            this.quickExitCode = -1;
        }
    }

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


    @Override
    public Optional<byte[]> getRawRom() {
        return Optional.of(SystemAdapter.readRawRom(romPath.toAbsolutePath()));
    }

    @Override
    public Optional<Path> getRomPath() {
        return Optional.of(romPath.toAbsolutePath());
    }

    @Override
    public Optional<System> getSystem() {
        return this.system;
    }

    public int getExitCode() {
        return this.quickExitCode;
    }

}
