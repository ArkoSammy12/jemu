package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public abstract class NESCartridge<E extends NESEmulator> implements Bus {

    private final E emulator;
    private final INESFile iNESFile;

    public NESCartridge(E emulator, INESFile iNESFile) {
        this.emulator = emulator;
        this.iNESFile = iNESFile;
    }

    public static <E extends NESEmulator> NESCartridge<E> getCartridge(E emulator, INESFile iNESFile) {
        return new NROMCartridge<>(emulator, iNESFile);
    }

    public INESFile getINESFile() {
        return this.iNESFile;
    }

    abstract public int readBytePPU(int address);

    abstract public void writeBytePPU(int address, int value);

    abstract public boolean mapsCIRAM();

}
