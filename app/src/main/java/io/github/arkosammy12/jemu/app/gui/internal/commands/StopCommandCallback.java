package io.github.arkosammy12.jemu.app.gui.internal.commands;

import io.github.arkosammy12.jemu.app.gui.commands.StopEmulatorCommand;

public non-sealed interface StopCommandCallback extends EmulatorCommandCallback {

    void onStop(StopEmulatorCommand stopEmulatorCommand);

}
