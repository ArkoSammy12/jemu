package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.PauseEmulatorCommand;

public non-sealed interface PauseCommandCallback extends EmulatorCommandCallback {

    void onPause(PauseEmulatorCommand pauseEmulatorCommand);

}
