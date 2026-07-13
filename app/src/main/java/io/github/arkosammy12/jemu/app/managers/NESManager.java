package io.github.arkosammy12.jemu.app.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.adapters.NESAdapter;
import io.github.arkosammy12.jemu.app.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.app.util.System;

import javax.sound.sampled.LineUnavailableException;
import java.util.Optional;

public class NESManager implements SystemManager {

    @Override
    public SystemAdapter createSystem(Jemu jemu, System system) throws LineUnavailableException {
        return new NESAdapter(jemu, system, this);
    }

    @Override
    public String getName() {
        return "Nintendo Entertainment System";
    }

    @Override
    public String getId() {
        return "nes";
    }

    @Override
    public Optional<String[]> getFileExtensions() {
        return Optional.of(new String[] {"nes"});
    }

}
