package io.github.arkosammy12.jemu.frontend.gui;

import io.github.arkosammy12.jemu.frontend.gui.commands.EmulatorCommand;

public sealed interface PendingEmulatorCommand permits MainWindow.PendingEmulatorCommandImpl {

    EmulatorCommand getEmulatorCommand();

    void acknowledge();

}
