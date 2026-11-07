package io.github.arkosammy12.jemu.frontend.util;

import io.github.arkosammy12.jemu.frontend.gui.commands.EmulatorCommand;
import io.github.arkosammy12.jemu.frontend.util.internal.PendingEmulatorCommandImpl;

public sealed interface PendingEmulatorCommand permits PendingEmulatorCommandImpl {

    EmulatorCommand getEmulatorCommand();

    void acknowledge();

}
