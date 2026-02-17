package io.github.arkosammy12.jemu.backend.common;

import io.github.arkosammy12.jemu.backend.drivers.KeyMapping;

import java.util.Collection;

public abstract class SystemController<E extends Emulator> {

    protected final E emulator;

    public SystemController(E emulator) {
        this.emulator = emulator;
    }

    abstract public Collection<KeyMapping> getKeyMappings();

}
