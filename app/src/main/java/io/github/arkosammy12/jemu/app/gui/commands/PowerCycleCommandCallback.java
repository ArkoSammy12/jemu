package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface PowerCycleCommandCallback extends EmulatorCommandCallback {

    void onReset(PowerCycleCommand powerCycleCommand);

}
