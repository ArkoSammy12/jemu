package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.StepCycleEmulatorCommand;

public non-sealed interface StepCycleCommandCallback extends EmulatorCommandCallback {

    void onStepCycle(StepCycleEmulatorCommand stepCycleEmulatorCommand);

}
