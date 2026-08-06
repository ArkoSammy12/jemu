package io.github.arkosammy12.jemu.app.system.rcastudioii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;

public class RCAStudioIIManager extends SystemManager {

    public RCAStudioIIManager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);
    }

    @Override
    public String getName() {
        return "RCA Studio II";
    }

    @Override
    public String getId() {
        return "rca-studioii";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of("st2");
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        return new RCAStudioIIAdapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof RCAStudioIIAdapter;
    }

}
