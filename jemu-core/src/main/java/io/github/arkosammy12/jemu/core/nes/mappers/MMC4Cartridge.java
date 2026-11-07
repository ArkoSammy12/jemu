package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public class MMC4Cartridge<E extends NESEmulator> extends MMC2Cartridge<E> {

    public MMC4Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x3FFF) {
            return (this.prgRomBankSelect << 14) | (address & 0x3FFF);
        } else {
            return (0b1111 << 14) | (address & 0x3FFF);
        }
    }

    @Override
    protected int mapChrAddress(int address, boolean isRead) {
        address &= 0x1FFF;
        if (address <= 0xFFF) {
            int mappedAddress = switch (this.latch0) {
                case FD -> (this.chrRomFDLowerBankSelect << 12) | (address & 0xFFF);
                case FE -> (this.chrRomFELowerBankSelect << 12) | (address & 0xFFF);
            };
            if (isRead) {
                if (address >= 0x0FD8 && address <= 0x0FDF) {
                    this.latch0 = CHRBankLatch.FD;
                } else if (address >= 0x0FE8 && address <= 0x0FEF) {
                    this.latch0 = CHRBankLatch.FE;
                }
            }
            return mappedAddress;
        } else {
            return super.mapChrAddress(address, isRead);
        }
    }

}
