package io.github.arkosammy12.jemu.app.gui.internal.commands;

import io.github.arkosammy12.jemu.app.gui.commands.ResetEmulatorCommand;

public non-sealed interface ResetEmulatorCommandCallback extends EmulatorCommandCallback {

    void onReset(ResetEmulatorCommand resetEmulatorCommand);

}
