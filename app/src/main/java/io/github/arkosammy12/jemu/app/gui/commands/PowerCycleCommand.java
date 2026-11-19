package io.github.arkosammy12.jemu.app.gui.commands;

import io.github.arkosammy12.jemu.app.gui.system.SystemDescriptor;
import org.jetbrains.annotations.NotNull;

public record PowerCycleCommand(@NotNull SystemDescriptor systemDescriptor, boolean systemDescriptorAutomaticallyDetected, boolean powerCycleIntoPaused) implements EmulatorCommand {}
