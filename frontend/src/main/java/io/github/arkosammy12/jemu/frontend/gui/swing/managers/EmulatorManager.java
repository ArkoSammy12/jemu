package io.github.arkosammy12.jemu.frontend.gui.swing.managers;

import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

public interface EmulatorManager {

    void setCurrentSystemDescriptor(@Nullable SystemDescriptor systemDescriptor);

}
