package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.NESPPU.*;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_MIRROR_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_MIRROR_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_START;
import static io.github.arkosammy12.jemu.core.nes.ines.INESFile.KB_8;

public class AXROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    //private final int[][] programRom;
    private final int[] programRom;
    private final int[] characterRom;
    private final int[] characterRam;

    private final int programRomMask;
    private int bankSelect;

    public AXROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        int[] programRomData = iNESFile.getProgramRom();
        this.programRom = Arrays.copyOf(programRomData, programRomData.length);
        this.programRomMask = this.programRom.length - 1;

        Optional<int[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterRom = null;
            this.characterRam = new int[KB_8];
        } else {
            int[] characterRomData = characterRomOptional.get();
            int characterRomSize = Math.clamp(characterRomData.length, 0, KB_8);
            this.characterRom = new int[characterRomSize];
            System.arraycopy(characterRomData, 0, this.characterRom, 0, this.characterRom.length);
            this.characterRam = null;
        }

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            if (this.characterRom == null) {
                return this.characterRam[address % this.characterRam.length];
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
            throw new EmulatorException("Invalid NES AXROM cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            if (this.characterRam != null) {
                this.characterRam[address % this.characterRam.length] = value & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= CIRAM_MIRROR_START && address <= CIRAM_MIRROR_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else if (address >= PALETTE_RAM_MIRROR_START && address <= PALETTE_RAM_MIRROR_END) {

        } else {
            throw new EmulatorException("Invalid NES AXROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        // Single-screen mirroring
        return (address & 0x3FF) | ((this.bankSelect & (1 << 4)) != 0 ? 0x400 : 0);
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return this.programRom[(((this.bankSelect & 0b111) << 15) | (address & 0x7FFF)) & this.programRomMask];
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            this.bankSelect = value & 0xFF;
        }
    }

}
