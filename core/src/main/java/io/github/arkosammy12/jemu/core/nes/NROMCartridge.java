package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.NESPPU.*;
import static io.github.arkosammy12.jemu.core.nes.ines.INESFile.KB_16;
import static io.github.arkosammy12.jemu.core.nes.ines.INESFile.KB_8;

public class NROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final int[] programRom;
    private final int[] characterRom;
    private final int[] programRam;

    public NROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        int programRamSize = Math.clamp(iNESFile.getProgramRamSize(), 0, KB_8);
        this.programRam = new int[programRamSize];

        int[] programRomData = iNESFile.getProgramRom();
        int programRomSize = Math.clamp(programRomData.length, 0, KB_16 * 2);
        this.programRom = new int[programRomSize];
        System.arraycopy(programRomData, 0, this.programRom, 0, this.programRom.length);

        Optional<int[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterRom = null;
        } else {
            int[] characterRomData = characterRomOptional.get();
            int characterRomSize = Math.clamp(characterRomData.length, 0, KB_8);
            this.characterRom = new int[characterRomSize];
            System.arraycopy(characterRomData, 0, this.characterRom, 0, this.characterRom.length);
        }

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            if (this.characterRom == null) {
                return 0xFF;
            } else {
                return this.characterRom[address % this.characterRom.length];
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= CIRAM_MIRROR_START && address <= CIRAM_MIRROR_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else if (address >= PALETTE_RAM_MIRROR_START && address <= PALETTE_RAM_MIRROR_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES MMC0 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {

        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= CIRAM_MIRROR_START && address <= CIRAM_MIRROR_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else if (address >= PALETTE_RAM_MIRROR_START && address <= PALETTE_RAM_MIRROR_END) {

        } else {
            throw new EmulatorException("Invalid NES MMC0 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            return this.programRam[(address - 0x6000) % this.programRam.length];
        } else if (address >= 0x8000 && address <= 0xBFFF) {
            return this.programRom[(address - 0x8000) % this.programRom.length];
        } else if (address >= 0xC000 && address <= 0xFFFF) {
            return this.programRom[(address - 0x8000) % this.programRom.length];
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            this.programRam[(address - 0x6000) % this.programRam.length] = value & 0xFF;
        } else if (address >= 0x8000 && address <= 0xBFFF) {

        } else if (address >= 0xC000 && address <= 0xFFFF) {

        }
    }
}
