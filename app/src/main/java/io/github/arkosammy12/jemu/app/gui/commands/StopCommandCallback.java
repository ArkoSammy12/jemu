package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface StopCommandCallback extends EmulatorCommandCallback {

    void onStop(StopEmulatorCommand stopEmulatorCommand);

}
