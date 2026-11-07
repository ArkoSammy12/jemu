package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.*;

public class NROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    public NROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
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
            return this.readByteVRAM(this.mapNametableAddress(address));
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
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES NROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null) {
                return (int) this.programRAM[(address & 0x1FFF) % this.programRAM.length] & 0xFF;
            } else {
                return -1;
            }
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[(address & 0x7FFF) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null) {
                this.programRAM[(address & 0x1FFF) % this.programRAM.length] = (byte) value;
            }
        }
    }

    @Override
    protected Optional<byte[]> getNonVolatilePrgRam() {
        return Optional.ofNullable(this.programRAM);
    }

    @Override
    protected Optional<byte[]> getNonVolatileChrRam() {
        return Optional.ofNullable(this.characterRAM);
    }

}
