package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.app.gui.commands.EmulatorCommand;

public final class PendingEmulatorCommand {

    private final EmulatorCommand emulatorCommand;
    private final Runnable acknowledgeFunction;

    public PendingEmulatorCommand(EmulatorCommand emulatorCommand, Runnable acknowledgeFunction) {
        this.emulatorCommand = emulatorCommand;
        this.acknowledgeFunction = acknowledgeFunction;
    }

    public EmulatorCommand getEmulatorCommand() {
        return this.emulatorCommand;
    }

    public void acknowledge() {
        this.acknowledgeFunction.run();
    }

}
