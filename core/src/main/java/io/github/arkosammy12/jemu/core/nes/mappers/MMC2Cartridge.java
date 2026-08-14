package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class MMC2Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    protected int prgRomBankSelect;
    protected int chrRomFDLowerBankSelect;
    protected int chrRomFELowerBankSelect;
    private int chrRomFDUpperBankSelect;
    private int chrRomFEUpperBankSelect;
    private NametableArrangement nametableArrangement = NametableArrangement.HORIZONTAL;

    protected CHRBankLatch latch0 = CHRBankLatch.FD;
    private CHRBankLatch latch1 = CHRBankLatch.FD;

    public MMC2Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[this.mapChrAddress(address, true) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[this.mapChrAddress(address, true) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES MMC2 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_START && address <= CHR_END) {
            if (this.characterRAM != null) {
                this.characterRAM[this.mapChrAddress(address, false) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_END) {

        } else {
            throw new EmulatorException("Invalid NES MMC2 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, this.nametableArrangement);
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
            return (int) this.programROM[this.mapPrgRomAddress(address)] & 0xFF;
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
        } else {
            switch (address & 0xF000) {
                case 0xA000 -> this.prgRomBankSelect = value & 0xF;
                case 0xB000 -> this.chrRomFDLowerBankSelect = value & 0x1F;
                case 0xC000 -> this.chrRomFELowerBankSelect = value & 0x1F;
                case 0xD000 -> this.chrRomFDUpperBankSelect = value & 0x1F;
                case 0xE000 -> this.chrRomFEUpperBankSelect = value & 0x1F;
                case 0xF000 -> this.nametableArrangement = (value & 1) != 0 ? NametableArrangement.VERTICAL : NametableArrangement.HORIZONTAL;
            }
        }
    }

    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x1FFF) {
            return (this.prgRomBankSelect << 13) | (address & 0x1FFF);
        } else if (address <= 0x3FFF) {
            return (0b1101 << 13) | (address & 0x1FFF);
        } else if (address <= 0x5FFF) {
            return (0b1110 << 13) | (address & 0x1FFF);
        } else {
            return (0b1111 << 13) | (address & 0x1FFF);
        }
    }

    protected int mapChrAddress(int address, boolean isRead) {
        address &= 0x1FFF;
        int mappedAddress;
        if (address <= 0xFFF) {
            mappedAddress = switch (this.latch0) {
                case FD -> (this.chrRomFDLowerBankSelect << 12) | (address & 0xFFF);
                case FE -> (this.chrRomFELowerBankSelect << 12) | (address & 0xFFF);
            };
            if (isRead) {
                if (address == 0x0FD8) {
                    this.latch0 = CHRBankLatch.FD;
                } else if (address == 0x0FE8) {
                    this.latch0 = CHRBankLatch.FE;
                }
            }
        } else {
            mappedAddress = switch (this.latch1) {
                case FD -> (this.chrRomFDUpperBankSelect << 12) | (address & 0xFFF);
                case FE -> (this.chrRomFEUpperBankSelect << 12) | (address & 0xFFF);
            };
            if (isRead) {
                if (address >= 0x1FD8 && address <= 0x1FDF) {
                    this.latch1 = CHRBankLatch.FD;
                } else if (address >= 0x1FE8 && address <= 0x1FEF) {
                    this.latch1 = CHRBankLatch.FE;
                }
            }
        }
        return mappedAddress;
    }

    @Override
    protected Optional<byte[]> getNonVolatilePrgRam() {
        return Optional.ofNullable(this.programRAM);
    }

    @Override
    protected Optional<byte[]> getNonVolatileChrRam() {
        return Optional.ofNullable(this.characterRAM);
    }

    protected enum CHRBankLatch {
        FD,
        FE
    }

}
