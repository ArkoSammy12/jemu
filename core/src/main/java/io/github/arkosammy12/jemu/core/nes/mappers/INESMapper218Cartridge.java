package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Arrays;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class INESMapper218Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programROM;
    private final CIRAMWiring ciramWiring;

    public INESMapper218Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programROM = Arrays.copyOf(programRomData, programRomData.length);

        if (iNESFile.hasAlternativeNametableLayout()) {
            if (iNESFile.getNametableArrangement()) {
                ciramWiring = CIRAMWiring.PPU_A13;
            } else {
                ciramWiring = CIRAMWiring.PPU_A12;
            }
        } else {
            if (iNESFile.getNametableArrangement()) {
                ciramWiring = CIRAMWiring.PPU_A10;
            } else {
                ciramWiring = CIRAMWiring.PPU_A11;
            }
        }
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES NROM cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES NROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return switch (this.ciramWiring) {
            case PPU_A10 -> (address & (1 << 10)) | (address & 0x3FF);
            case PPU_A11 -> ((address & (1 << 11)) >>> 1) | (address & 0x3FF);
            case PPU_A12 -> ((address & (1 << 12)) >>> 2) | (address & 0x3FF);
            case PPU_A13 -> ((address & (1 << 13)) >>> 3) | (address & 0x3FF);
        };
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            return -1;
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[(address - 0x8000) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {

    }

    private enum CIRAMWiring {
        PPU_A10,
        PPU_A11,
        PPU_A12,
        PPU_A13
    }

}
