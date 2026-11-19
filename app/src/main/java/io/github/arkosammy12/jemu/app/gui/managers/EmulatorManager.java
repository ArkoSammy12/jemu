package io.github.arkosammy12.jemu.app.gui.managers;

import io.github.arkosammy12.jemu.app.gui.system.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

public interface EmulatorManager {

    void setCurrentSystemDescriptor(@Nullable SystemDescriptor systemDescriptor);

}
