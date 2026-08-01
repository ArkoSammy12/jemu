package io.github.arkosammy12.jemu.app.system.appleii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;

import java.util.Collection;
import java.util.List;

public class AppleIIManager extends SystemManager {

    public AppleIIManager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);
    }

    @Override
    public SystemAdapter createSystem() throws Exception {
        return new AppleIIAdapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof AppleIIAdapter;
    }

    @Override
    public String getName() {
        return "Apple II";
    }

    @Override
    public String getId() {
        return "appleii";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of();
    }
}
