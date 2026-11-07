package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.commands.PowerCycleCommand;

public non-sealed interface PowerCycleCommandCallback extends EmulatorCommandCallback {

    void onReset(PowerCycleCommand powerCycleCommand);

}
