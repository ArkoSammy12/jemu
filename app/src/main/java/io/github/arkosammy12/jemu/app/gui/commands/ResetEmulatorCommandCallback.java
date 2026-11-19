package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface ResetEmulatorCommandCallback extends EmulatorCommandCallback {

    void onReset(ResetEmulatorCommand resetEmulatorCommand);

}
