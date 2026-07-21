package io.github.arkosammy12.jemu.core.exceptions;

import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.common.SystemHost;

public class MissingROMException extends EmulatorException {

    public MissingROMException(Emulator emulator) {
        this(emulator.getHost());
    }

    public MissingROMException(SystemHost systemHost) {
        super(systemHost.getSystemName() + " requires a ROM file to start!");
    }

}
