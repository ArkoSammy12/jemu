package io.github.arkosammy12.jemu.app.gui.internal.commands;

import io.github.arkosammy12.jemu.app.gui.commands.PowerCycleCommand;

public non-sealed interface PowerCycleCommandCallback extends EmulatorCommandCallback {

    void onReset(PowerCycleCommand powerCycleCommand);

}
