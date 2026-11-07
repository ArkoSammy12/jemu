package io.github.arkosammy12.jemu.frontend.gui.commands;

public sealed interface EmulatorCommand permits PauseEmulatorCommand, PowerCycleCommand, ResetEmulatorCommand, StepCycleEmulatorCommand, StepFrameEmulatorCommand, StopEmulatorCommand {}
