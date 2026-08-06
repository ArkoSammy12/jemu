package io.github.arkosammy12.jemu.frontend.gui.commands;

import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import org.jetbrains.annotations.NotNull;

public record PowerCycleCommand(@NotNull SystemDescriptor systemDescriptor, boolean systemDescriptorFromAutomaticDetection, boolean powerCycleIntoPaused) implements EmulatorCommand {}
