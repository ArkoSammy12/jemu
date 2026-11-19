package io.github.arkosammy12.jemu.app.util;

import io.github.arkosammy12.jemu.app.gui.commands.EmulatorCommand;
import io.github.arkosammy12.jemu.app.util.internal.PendingEmulatorCommandImpl;

public sealed interface PendingEmulatorCommand permits PendingEmulatorCommandImpl {

    EmulatorCommand getEmulatorCommand();

    void acknowledge();

}
