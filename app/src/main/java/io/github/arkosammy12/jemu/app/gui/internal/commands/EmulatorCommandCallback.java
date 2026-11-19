package io.github.arkosammy12.jemu.app.gui.internal.commands;

public sealed interface EmulatorCommandCallback permits PauseCommandCallback, PowerCycleCommandCallback, ResetEmulatorCommandCallback, StepCycleCommandCallback, StepFrameCommandCallback, StopCommandCallback {}
