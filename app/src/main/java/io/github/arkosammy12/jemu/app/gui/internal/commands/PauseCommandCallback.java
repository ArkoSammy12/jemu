package io.github.arkosammy12.jemu.app.gui.internal.commands;

import io.github.arkosammy12.jemu.app.gui.commands.PauseEmulatorCommand;

public non-sealed interface PauseCommandCallback extends EmulatorCommandCallback {

    void onPause(PauseEmulatorCommand pauseEmulatorCommand);

}
