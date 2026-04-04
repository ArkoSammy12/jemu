package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_START;

public abstract class NESCartridge<E extends NESEmulator> implements Bus {

    protected final E emulator;
    protected final INESFile iNESFile;

    private final int[] vRam = new int[0x800];

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

    protected int readByteVRAM(int address) {
        return this.vRam[this.mapNametableAddress(address)];
    }

    protected void writeByteVRAM(int address, int value) {
        this.vRam[this.mapNametableAddress(address)] = value & 0xFF;
    }

    private int mapNametableAddress(int address) {
        int vRamAddr = (address - CIRAM_START) & 0x0FFF;

        int ppuA10 = (vRamAddr >> 10) & 1;
        int ppuA11 = (vRamAddr >> 11) & 1;

        int ciRamA10;
        if (this.getINESFile().getNametableMirroring() == INESFile.NametableArrangement.VERTICAL) {
            ciRamA10 = ppuA11;
        } else {
            ciRamA10 = ppuA10;
        }

        return (ciRamA10 << 10) | (vRamAddr & 0x03FF);
    }

    public boolean getIRQSignal() {
        return false;
    }

}
