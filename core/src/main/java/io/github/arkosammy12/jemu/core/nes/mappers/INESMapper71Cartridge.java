package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.function.Supplier;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class INESMapper71Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final Supplier<NametableArrangement> nametableArrangementSupplier;

    private NametableArrangement nametableArrangement = NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
    private int bankSelect;

    public INESMapper71Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
        if (iNESFile.getSubmapperNumber() == 1) {
            this.nametableArrangementSupplier = () -> this.nametableArrangement;
        } else {
            this.nametableArrangementSupplier = () -> this.iNESFileNametableArrangement;
        }
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[(address & 0x1FFF) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[(address & 0x1FFF) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address, this.nametableArrangementSupplier.get()));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES NROM cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterRAM != null) {
                this.characterRAM[(address & 0x1FFF) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address, this.nametableArrangementSupplier.get()), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES NROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x8000 && address <= 0x9FFF) {
            this.nametableArrangement = (value & (1 << 4)) != 0 ? NametableArrangement.SINGLE_SCREEN_UPPER_BANK : NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
        } else if (address >= 0xC000 && address <= 0xFFFF) {
            this.bankSelect = value & 0xF;
        }
    }

    private int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x3FFF) {
            return (address & 0x3FFF) | (this.bankSelect << 14);
        } else {
            return (address & 0x3FFF) | (0b1111 << 14);
        }
    }

}
