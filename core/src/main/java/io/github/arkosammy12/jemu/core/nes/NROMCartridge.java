package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Optional;

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
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterRom == null) {
                return 0xFF;
            } else {
                return this.characterRom[address % this.characterRom.length];
            }
        } else {
            throw new EmulatorException("Invalid NES MMC0 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {

        } else {
            throw new EmulatorException("Invalid NES MMC0 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public boolean mapsCIRAM() {
        return false;
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
            return 0xFF;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            this.programRam[(address - 0x6000) % this.programRam.length] = value & 0xFF;
            // TODO: Remove Blargg test rom string printout when PPU is sufficiently implemented to read test results on-screen
            if (address == 0x6000 && value >= 0x00 && value <= 0x07) {
                int i = 0x6004;
                int ascii = this.programRam[(i - 0x6000) % this.programRam.length];
                while (ascii != 0) {
                    System.out.print((char) ascii);
                    i++;
                    ascii = this.programRam[(i - 0x6000) % this.programRam.length];
                }
            }
        } else if (address >= 0x8000 && address <= 0xBFFF) {

        } else if (address >= 0xC000 && address <= 0xFFFF) {

        }
    }
}
