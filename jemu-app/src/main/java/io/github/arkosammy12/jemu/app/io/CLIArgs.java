package io.github.arkosammy12.jemu.app.io;

import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Optional;

@CommandLine.Command(
        name = "jemu",
        mixinStandardHelpOptions = true,
        versionProvider = MavenProperties.Provider.class,
        description = "Initializes jemu with the desired settings and starts emulation."
)
public final class CLIArgs {

    @CommandLine.Option(
            names = {"--rom", "-r"},
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "The path of the file containing the raw binary ROM data."
    )
    private Path romPath;

    @CommandLine.Option(
            names = {"--system", "-s"},
            defaultValue = CommandLine.Option.NULL_VALUE,
            description = "Launch with desired system selected or leave unspecified to use current setting."
    )
    private SystemManager system;

    private final boolean exitImmediately;

    public CLIArgs(String[] args, SystemRegistry systemRegistry) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Args array cannot be empty!");
        }
        CommandLine cli = new CommandLine(this);
        cli.registerConverter(SystemManager.class, systemRegistry.new SystemManagerConverter());
        CommandLine.ParseResult parseResult = cli.parseArgs(args);
        Integer executeHelpResult = CommandLine.executeHelpRequest(parseResult);
        int exitCodeOnUsageHelp = cli.getCommandSpec().exitCodeOnUsageHelp();
        int exitCodeOnVersionHelp = cli.getCommandSpec().exitCodeOnVersionHelp();
        this.exitImmediately = executeHelpResult != null && (executeHelpResult == exitCodeOnUsageHelp || executeHelpResult == exitCodeOnVersionHelp);
    }

    public Optional<Path> getRomPath() {
        return Optional.ofNullable(this.romPath);
    }

    public Optional<SystemManager> getSystem() {
        return Optional.ofNullable(this.system);
    }

    public boolean exitImmediately() {
        return this.exitImmediately;
    }

}
