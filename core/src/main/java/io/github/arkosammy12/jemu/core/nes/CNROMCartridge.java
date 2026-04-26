package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.NESPPU.CHR_ROM_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CHR_ROM_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_MIRROR_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_MIRROR_START;
import static io.github.arkosammy12.jemu.core.nes.NESPPU.PALETTE_RAM_START;

public class CNROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final int[] programRom;
    private final int[] characterRom;
    private final int[] characterRam;

    private int bankSelect;

    public CNROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        int[] programRomData = iNESFile.getProgramRom();
        this.programRom = Arrays.copyOf(programRomData, programRomData.length);

        Optional<int[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterRom = null;
            this.characterRam = new int[iNESFile.getCharacterRamSize()];
        } else {
            int[] characterRomData = characterRomOptional.get();
            this.characterRom = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRam = null;
        }

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            if (this.characterRom == null) {
                return this.characterRam[address % this.characterRam.length];
            } else {
                return this.characterRom[(((this.bankSelect << 13) | address)) % this.characterRom.length];
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
            throw new EmulatorException("Invalid NES CNROM cartridge PPU read address $%04X!".formatted(address));
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
            throw new EmulatorException("Invalid NES CNROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    // TODO: CPU $6000-$7FFF: 2 KiB of PRG-RAM, mirrored three times (Hayauchi Super Igo only)

    @Override
    public int readByte(int address) {
        if (address >= 0x8000 && address <= 0xBFFF) {
            return this.programRom[(address - 0x8000) % this.programRom.length];
        } else if (address >= 0xC000 && address <= 0xFFFF) {
            return this.programRom[(address - 0x8000) % this.programRom.length];
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            this.bankSelect = value & 3;
        }
    }

}
