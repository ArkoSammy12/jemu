package io.github.arkosammy12.jemu.frontend.gui.swing.commands;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ResetEmulatorCommand(@Nullable SystemDescriptor systemDescriptor, boolean resetIntoPaused) implements EmulatorCommand {

    public Optional<SystemDescriptor> getSystemDescriptor() {
        return Optional.ofNullable(this.systemDescriptor);
    }

}
