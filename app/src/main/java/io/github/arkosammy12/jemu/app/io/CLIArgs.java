package io.github.arkosammy12.jemu.app.io;

import io.github.arkosammy12.jemu.app.Main;
import io.github.arkosammy12.jemu.app.util.System;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Optional;

@CommandLine.Command(
        name = "jemu",
        mixinStandardHelpOptions = true,
        version = Main.VERSION_STRING,
        description = "Initializes jemu with the desired settings and starts emulation."
)
public final class CLIArgs {

    @CommandLine.Option(
            names = {"--rom", "-r"},
            required = true,
            description = "The path of the file containing the raw binary ROM data."
    )
    private Path romPath;

    @CommandLine.Option(
            names = {"--system", "-s"},
            converter = io.github.arkosammy12.jemu.app.util.System.Converter.class,
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "Launch with desired system selected or leave unspecified to use current setting."
    )
    private io.github.arkosammy12.jemu.app.util.System system;

    private final boolean exitImmediately;

    public CLIArgs(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Args array cannot be empty!");
        }
        CommandLine cli = new CommandLine(this);
        CommandLine.ParseResult parseResult = cli.parseArgs(args);
        Integer executeHelpResult = CommandLine.executeHelpRequest(parseResult);
        int exitCodeOnUsageHelp = cli.getCommandSpec().exitCodeOnUsageHelp();
        int exitCodeOnVersionHelp = cli.getCommandSpec().exitCodeOnVersionHelp();
        this.exitImmediately = executeHelpResult != null && (executeHelpResult == exitCodeOnUsageHelp || executeHelpResult == exitCodeOnVersionHelp);
    }

    public Path getRomPath() {
        return this.romPath;
    }

    public Optional<System> getSystem() {
        return Optional.ofNullable(this.system);
    }

    public boolean exitImmediately() {
        return this.exitImmediately;
    }

}
