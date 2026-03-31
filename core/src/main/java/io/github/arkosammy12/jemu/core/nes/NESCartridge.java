package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;

public abstract class NESCartridge implements Bus {

    public NESCartridge() {

    }

    public static NESCartridge getCartridge(NESEmulator emulator) {
        return null;
    }

}
