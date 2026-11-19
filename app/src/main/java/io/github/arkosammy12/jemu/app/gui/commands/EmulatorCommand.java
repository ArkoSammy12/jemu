package io.github.arkosammy12.jemu.app.gui.commands;

public sealed interface EmulatorCommand permits PauseEmulatorCommand, PowerCycleCommand, ResetEmulatorCommand, StepCycleEmulatorCommand, StepFrameEmulatorCommand, StopEmulatorCommand {}
