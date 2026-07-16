package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nes.ines.NES20File;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;
import static io.github.arkosammy12.jemu.core.util.ByteSizes.KB_16;

public class MMC1Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programROM;
    private final byte[] programRAM;
    private final byte[] characterROM;
    private final byte[] characterRAM;

    private boolean loadRegisterWrittenOnThisCycle;
    private boolean loadRegisterIgnoreWrites;
    private int loadRegisterWriteCounter;

    private int loadRegister;
    private int control = 0x0C;
    private int chrBank0;
    private int chrBank1;
    private int prgBank;

    public MMC1Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programROM = Arrays.copyOf(programRomData, programRomData.length);

        int programRamSize = iNESFile.getProgramRamSize();
        if (iNESFile instanceof NES20File nes20File) {
            programRamSize += nes20File.getNonVolatileProgramRamSizeBytes();
        }

        this.programRAM = programRamSize > 0 ? new byte[programRamSize] : null;

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

        this.restoreSaveData(this.programRAM, this.characterRAM);

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[this.mapChrAddress(address) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES MMC1 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterRAM != null) {
                this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES MMC1 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, switch (this.getNametableArrangement()) {
            case 0 -> NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
            case 1 -> NametableArrangement.SINGLE_SCREEN_UPPER_BANK;
            case 2 -> NametableArrangement.HORIZONTAL;
            case 3 -> NametableArrangement.VERTICAL;
            default -> throw new EmulatorException("NES MMC1 nametable arrangement bits not in [0, 3]!");
        });
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null) {
                return (int) this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] & 0xFF;
            } else {
                return -1;
            }
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.isProgramRamEnabled()) {
                this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
            }
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            if ((value & 0x80) != 0) {
                this.loadRegister = 0;
                this.loadRegisterWriteCounter = 0;
                this.control |= 0xC;
                this.loadRegisterIgnoreWrites = true;
                this.loadRegisterWrittenOnThisCycle = true;
            } else if (!this.loadRegisterIgnoreWrites) {
                this.loadRegisterWrittenOnThisCycle = true;
                this.loadRegisterIgnoreWrites = true;
                if (this.loadRegisterWriteCounter >= 4) {
                    int writeValue = ((value & 1) << 4) | (this.loadRegister & 0xF);
                    switch (address & 0x6000) {
                        case 0x0000 -> this.control = writeValue;
                        case 0x2000 -> this.chrBank0 = writeValue;
                        case 0x4000 -> this.chrBank1 = writeValue;
                        case 0x6000 -> this.prgBank = writeValue;
                    }
                    this.loadRegister = 0;
                    this.loadRegisterWriteCounter = 0;
                } else {
                    this.loadRegister = ((value & 1) << 3) | (this.loadRegister >>> 1);
                    this.loadRegisterWriteCounter++;
                }
            }
        }
    }

    @Override
    public void cycle() {
        if (!this.loadRegisterWrittenOnThisCycle && this.loadRegisterIgnoreWrites) {
            this.loadRegisterIgnoreWrites = false;
        }
        this.loadRegisterWrittenOnThisCycle = false;
    }

    private int getNametableArrangement() {
        return this.control & 0b11;
    }

    private int getProgramRomBankMode() {
        return (this.control >>> 2) & 0b11;
    }

    private boolean isProgramRamEnabled() {
        return (this.prgBank & (1 << 4)) == 0;
    }

    private int mapChrAddress(int address) {
        address &= 0x1FFF;
        if ((this.control & (1 << 4)) == 0) {
            return ((this.chrBank0 & 0b11110) << 12) | (address);
        } else {
            if (address <= 0x0FFF) {
                return ((this.chrBank0 & 0b11111) << 12) | (address & 0x0FFF);
            } else {
                return ((this.chrBank1 & 0b11111) << 12) | (address & 0x0FFF);
            }
        }
    }

    private int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        int prgRomA18;
        if ((this.control & (1 << 4)) == 0 || address <= 0x0FFF) {
            prgRomA18 = (this.chrBank0 & (1 << 4)) << 14;
        } else {
            prgRomA18 = (this.chrBank1 & (1 << 4)) << 14;
        }

        int bankBits = this.prgBank & 0b1111;
        return switch (this.getProgramRomBankMode()) {
            case 0, 1 -> address | ((bankBits & ~1) << 14) | (prgRomA18);
            case 2 -> {
                if (address <= 0x3FFF) {
                    bankBits = 0;
                }
                yield (address & 0x3FFF) | (bankBits << 14) | (prgRomA18);
            }
            case 3 -> {
                if (address > 0x3FFF) {
                    bankBits = 0b1111;
                }
                yield (address & 0x3FFF) | (bankBits << 14) | (prgRomA18);
            }
            default -> throw new EmulatorException("NES MMC1 PRG-ROM bankBits mode bits not in [0, 3]!");
        };
    }

    private int mapPrgRamAddress(int address) {
        address &= 0x1FFF;
        int bankBits;
        if ((this.control & (1 << 4)) == 0 || address <= 0x0FFF) {
            bankBits = (this.chrBank0 >>> 2) & 0b11;
        } else {
            bankBits = (this.chrBank1 >>> 2) & 0b11;
        }
        if (this.programRAM.length <= KB_16) {
            bankBits >>>= 1;
        }
        return address | (bankBits << 13);
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
