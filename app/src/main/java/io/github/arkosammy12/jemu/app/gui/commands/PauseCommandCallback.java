package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface PauseCommandCallback extends EmulatorCommandCallback {

    void onPause(PauseEmulatorCommand pauseEmulatorCommand);

}
