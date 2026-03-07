package io.github.arkosammy12.jemu.frontend.swing.events;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Optional;
import java.util.function.Supplier;

public non-sealed class ResetEvent implements Event {

    @Nullable
    private final SystemDescriptor systemDescriptor;

    private final boolean resetIntoPaused;

    public ResetEvent(@Nullable SystemDescriptor systemDescriptor, boolean resetIntoPaused) {
        this.systemDescriptor = systemDescriptor;
        this.resetIntoPaused = resetIntoPaused;
    }

    public boolean resetIntoPaused() {
        return this.resetIntoPaused;
    }

    public void onCompleted(@Nullable Supplier<JPanel> systemDisplaySupplier) {

    }

    public Optional<SystemDescriptor> getSystemDescriptor() {
        return Optional.ofNullable(this.systemDescriptor);
    }

}
