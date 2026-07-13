package io.github.arkosammy12.jemu.app.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;

public interface SystemManager extends SystemDescriptor {

    SystemAdapter createSystem(Jemu jemu, System system) throws Exception;

}
