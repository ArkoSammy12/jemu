package io.github.arkosammy12.jemu.frontend.gui.swing.commands;

public sealed interface EmulatorCommand permits PauseEmulatorCommand, PowerCycleCommand, ResetEmulatorCommand, StepCycleEmulatorCommand, StepFrameEmulatorCommand, StopEmulatorCommand {}
