package io.github.arkosammy12.jemu.frontend.util.internal;

import io.github.arkosammy12.jemu.frontend.gui.commands.EmulatorCommand;
import io.github.arkosammy12.jemu.frontend.util.PendingEmulatorCommand;

public final class PendingEmulatorCommandImpl implements PendingEmulatorCommand {

    private final EmulatorCommand emulatorCommand;
    private final Runnable acknowledgeFunction;

    public PendingEmulatorCommandImpl(EmulatorCommand emulatorCommand, Runnable acknowledgeFunction) {
        this.emulatorCommand = emulatorCommand;
        this.acknowledgeFunction = acknowledgeFunction;
    }

    @Override
    public EmulatorCommand getEmulatorCommand() {
        return this.emulatorCommand;
    }

    @Override
    public void acknowledge() {
        this.acknowledgeFunction.run();
    }

}
