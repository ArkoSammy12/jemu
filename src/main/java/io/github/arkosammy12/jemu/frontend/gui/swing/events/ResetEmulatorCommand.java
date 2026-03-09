package io.github.arkosammy12.jemu.frontend.gui.swing.events;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ResetEmulatorCommand(@Nullable SystemDescriptor systemDescriptor, boolean resetIntoPaused) implements EmulatorCommand {

    public Optional<SystemDescriptor> getSystemDescriptor() {
        return Optional.ofNullable(this.systemDescriptor);
    }

    public non-sealed interface Callback extends EmulatorCommand.Callback {

        void onReset(ResetEmulatorCommand resetEmulatorCommand);

    }

}
