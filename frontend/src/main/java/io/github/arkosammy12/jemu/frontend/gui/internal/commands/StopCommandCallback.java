package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.StopEmulatorCommand;

public non-sealed interface StopCommandCallback extends EmulatorCommandCallback {

    void onStop(StopEmulatorCommand stopEmulatorCommand);

}
