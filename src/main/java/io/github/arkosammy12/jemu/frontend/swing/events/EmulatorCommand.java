package io.github.arkosammy12.jemu.frontend.swing.events;

public sealed interface EmulatorCommand permits PauseEmulatorCommand, ResetEmulatorCommand, StepCycleEmulatorCommand, StepFrameEmulatorCommand, StopEmulatorCommand {

    sealed interface Callback permits PauseEmulatorCommand.Callback, ResetEmulatorCommand.Callback, StepCycleEmulatorCommand.Callback, StepFrameEmulatorCommand.Callback, StopEmulatorCommand.Callback {}

}
