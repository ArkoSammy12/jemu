package io.github.arkosammy12.jemu.frontend.gui.managers;

import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

public interface EmulatorManager {

    void setCurrentSystemDescriptor(@Nullable SystemDescriptor systemDescriptor);

}
