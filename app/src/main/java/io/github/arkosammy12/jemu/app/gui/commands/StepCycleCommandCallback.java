package io.github.arkosammy12.jemu.app.gui.commands;

public non-sealed interface StepCycleCommandCallback extends EmulatorCommandCallback {

    void onStepCycle(StepCycleEmulatorCommand stepCycleEmulatorCommand);

}
