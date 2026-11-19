package io.github.arkosammy12.jemu.app.gui.commands;

import io.github.arkosammy12.jemu.app.gui.system.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ResetEmulatorCommand(@Nullable SystemDescriptor systemDescriptor, boolean systemDescriptorAutomaticallyDetected, boolean resetIntoPaused) implements EmulatorCommand {

    public Optional<SystemDescriptor> getSystemDescriptor() {
        return Optional.ofNullable(this.systemDescriptor);
    }

}
