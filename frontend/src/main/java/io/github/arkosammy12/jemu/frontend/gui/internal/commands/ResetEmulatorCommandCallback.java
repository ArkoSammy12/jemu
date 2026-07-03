package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.ResetEmulatorCommand;

public non-sealed interface ResetEmulatorCommandCallback extends EmulatorCommandCallback {

    void onReset(ResetEmulatorCommand resetEmulatorCommand);

}
