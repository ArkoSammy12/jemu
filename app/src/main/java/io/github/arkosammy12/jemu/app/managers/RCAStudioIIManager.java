package io.github.arkosammy12.jemu.app.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.RCAStudioIIAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class RCAStudioIIManager implements SystemManager {

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new RCAStudioIIAdapter(jemu, system, this);
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

}
