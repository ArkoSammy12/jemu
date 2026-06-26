package io.github.arkosammy12.jemu.frontend.gui.swing.commands;

import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import org.jetbrains.annotations.NotNull;

public record PowerCycleCommand(@NotNull SystemDescriptor systemDescriptor, boolean powerCycleIntoPaused) implements EmulatorCommand {}
