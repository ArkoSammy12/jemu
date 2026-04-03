package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class NESPPUBus<E extends NESEmulator> implements Bus {

    private final E emulator;

    public static final int CHR_ROM_START = 0x0000;
    public static final int CHR_ROM_END = 0x1FFF;

    public static final int CIRAM_START = 0x2000;
    public static final int CIRAM_END = 0x2FFF;

    public static final int PALETTE_RAM_START = 0x3F00;
    public static final int PALETTE_RAM_END = 0x3F1F;

    private final int[] vRam = new int[0x800];
    private final int[] paletteRam = new int[0x20];

    public NESPPUBus(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x3000 && address <= 0x3EFF) {
            address = 0x2000 + (address - 0x3000);
        } else if (address >= 0x3F20 && address <= 0x3FFF) {
            address = 0x3F00 + (address - 0x3F20);
        }

        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            // TODO: Read buffer
            int readBuffer = this.emulator.getCartridge().readBytePPU(address);
            return this.paletteRam[address - PALETTE_RAM_START];

        } else {
            throw new EmulatorException("Invalid NES PPU memory address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        if (address >= 0x3000 && address <= 0x3EFF) {
            address = 0x2000 + (address - 0x3000);
        } else if (address >= 0x3F20 && address <= 0x3FFF) {
            address = 0x3F00 + (address - 0x3F20);
        }

        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            this.paletteRam[(address - PALETTE_RAM_START) % this.paletteRam.length] = value;
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else {
            throw new EmulatorException("Invalid NES PPU memory address $%04X!".formatted(address));
        }
    }

    public int readByteVRAM(int address) {
        return this.vRam[address - CIRAM_START];
    }

    public void writeByteVRAM(int address, int value) {
        this.vRam[address - CIRAM_START] = value & 0xFF;
    }

}
