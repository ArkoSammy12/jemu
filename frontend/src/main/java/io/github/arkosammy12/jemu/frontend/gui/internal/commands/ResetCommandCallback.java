package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.ResetEmulatorCommand;

public non-sealed interface ResetCommandCallback extends EmulatorCommandCallback {

    void onReset(ResetEmulatorCommand resetEmulatorCommand);

}
