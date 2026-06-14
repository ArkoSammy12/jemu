package io.github.arkosammy12.jemu.core.nintendo.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nintendo.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nintendo.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nintendo.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nintendo.nes.ines.NES20File;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.*;
import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nintendo.nes.RP2C02.PALETTE_RAM_START;

public class AxROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programROM;
    private final byte[] characterROM;
    private final byte[] characterRAM;

    private int bankSelect;

    private final boolean hasBusConflicts;

    public AxROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programROM = Arrays.copyOf(programRomData, programRomData.length);

        Optional<byte[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterROM = null;
            int characterRamSize = iNESFile.getCharacterRamSize();
            if (iNESFile instanceof NES20File nes20File) {
                characterRamSize += nes20File.getNonVolatileCharacterRamSizeBytes();
            }
            this.characterRAM = new byte[characterRamSize];
        } else {
            byte[] characterRomData = characterRomOptional.get();
            this.characterROM = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRAM = null;
        }

        this.hasBusConflicts = iNESFile.getSubmapperNumber() == 2;

        this.restoreSaveData(null, this.characterRAM);

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[address % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[address % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES AXROM cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterRAM != null) {
                this.characterRAM[address % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES AXROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, (this.bankSelect & (1 << 4)) != 0 ? NametableArrangement.SINGLE_SCREEN_UPPER_BANK : NametableArrangement.SINGLE_SCREEN_LOWER_BANK);
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
        if (address >= 0x8000 && address <= 0xFFFF) {
            if (this.hasBusConflicts) {
                value &= (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
            }
            this.bankSelect = value & 0xFF;
        }
    }

    private int mapPrgRomAddress(int address) {
        return ((this.bankSelect & 0b111) << 15) | (address & 0x7FFF);
    }

    @Override
    protected Optional<byte[]> getChrRam() {
        return Optional.ofNullable(this.characterRAM);
    }

}
