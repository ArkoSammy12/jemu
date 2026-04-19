package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
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
        // TODO: Easy mappers (CNROM, UNROM, ANROM)
        int mapperNumber = iNESFile.getMapperNumber();
        return switch (mapperNumber) {
            case 0 -> new NROMCartridge<>(emulator, iNESFile);
            default -> throw new EmulatorException("Unimplemented iNES mapper number %d!".formatted(mapperNumber));
        };
    }

    public INESFile getINESFile() {
        return this.iNESFile;
    }

    abstract public int readBytePPU(int address);

    abstract public void writeBytePPU(int address, int value);

    protected int readByteVRAM(int address) {
        return this.vRam[address];
    }

    protected void writeByteVRAM(int address, int value) {
        this.vRam[address] = value & 0xFF;
    }

    protected int mapNametableAddress(int address) {
        int vRamAddr = (address - CIRAM_START) & 0x0FFF;

        int ppuA10 = (vRamAddr >> 10) & 1;
        int ppuA11 = (vRamAddr >> 11) & 1;

        int ciRamA10 = switch (this.getINESFile().getNametableMirroring()) {
            case VERTICAL -> ppuA11;
            case HORIZONTAL -> ppuA10;
        };


        return (ciRamA10 << 10) | (vRamAddr & 0x03FF);
    }

    public boolean getIRQSignal() {
        return false;
    }

}
