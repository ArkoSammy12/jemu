package io.github.arkosammy12.jemu.frontend.swing.events;

public record StepCycleEmulatorCommand() implements EmulatorCommand {

    public non-sealed interface Callback extends EmulatorCommand.Callback {

        void onStepCycle(StepCycleEmulatorCommand stepCycleEmulatorCommand);

    }

}
