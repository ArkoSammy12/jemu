package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public class MMC6Cartridge<E extends NESEmulator> extends MMC3Cartridge<E> {

    private boolean prgRamEnabled;
    private boolean allowPrgRamWritesFirstHalf;
    private boolean allowPrgRamReadsFirstHalf;
    private boolean allowPrgRamWritesSecondHalf;
    private boolean allowPrgRamReadsSecondHalf;

    public MMC6Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM == null || address <= 0x6FFF || !this.isPrgRamEnabled()) {
                return -1;
            }
            address &= 0x3FF;
            if (address <= 0x1FF) {
                if (this.allowPrgRamReadsFirstHalf()) {
                    return (int) this.programRAM[address % this.programRAM.length] & 0xFF;
                } else if (this.allowPrgRamReadsSecondHalf()) {
                    return 0x00;
                } else {
                    return -1;
                }
            } else {
                if (this.allowPrgRamReadsSecondHalf()) {
                    return (int) this.programRAM[address % this.programRAM.length] & 0xFF;
                } else if (this.allowPrgRamReadsFirstHalf()) {
                    return 0x00;
                } else {
                    return -1;
                }
            }
        } else {
            return super.readByte(address);
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM == null || address <= 0x6FFF || !this.isPrgRamEnabled()) {
                return;
            }
            address &= 0x3FF;
            if (address <= 0x1FF) {
                if (this.allowPrgRamWritesFirstHalf() && this.allowPrgRamReadsFirstHalf()) {
                    this.programRAM[address % this.programRAM.length] = (byte) value;
                }
            } else {
                if (this.allowPrgRamWritesSecondHalf() && this.allowPrgRamReadsSecondHalf()) {
                    this.programRAM[address % this.programRAM.length] = (byte) value;
                }
            }
        } else {
            super.writeByte(address, value);
        }
    }


    protected void setBankSelect(int value) {
        super.setBankSelect(value);
        this.prgRamEnabled = (value & (1 << 5)) != 0;
    }

    @Override
    protected void setPrgRamProtect(int value) {
        super.setPrgRamProtect(value);
        this.allowPrgRamWritesFirstHalf = (value & (1 << 4)) != 0;
        this.allowPrgRamReadsFirstHalf = (value & (1 << 5)) != 0;
        this.allowPrgRamWritesSecondHalf = (value & (1 << 6)) != 0;
        this.allowPrgRamReadsSecondHalf = (value & (1 << 7)) != 0;
    }

    protected boolean isPrgRamEnabled() {
        return this.prgRamEnabled;
    }

    private boolean allowPrgRamWritesFirstHalf() {
        return this.allowPrgRamWritesFirstHalf;
    }

    private boolean allowPrgRamReadsFirstHalf() {
        return this.allowPrgRamReadsFirstHalf;
    }

    private boolean allowPrgRamWritesSecondHalf() {
        return this.allowPrgRamWritesSecondHalf;
    }

    private boolean allowPrgRamReadsSecondHalf() {
        return this.allowPrgRamReadsSecondHalf;
    }

    // If making changes to this, also change in MMC3 if needed
    @Override
    public void observePPUAddress(int address) {
        if (address != this.previousPPUAddress) {
            if ((address & A12) != 0 && (this.previousPPUAddress & A12) == 0 && this.cyclesDown >= 4) {
                boolean reloadIrqCounter = this.irqCounter <= 0 || this.irqReload;
                boolean reloadedToZero = this.irqReload && this.irqCounterReload == 0;
                if (reloadIrqCounter) {
                    this.irqCounter = this.irqCounterReload;
                    this.irqReload = false;
                } else {
                    this.irqCounter--;
                }
                boolean decrementedToZero = !reloadIrqCounter && this.irqCounter == 0;
                if ((reloadedToZero || decrementedToZero) && this.irqEnabled) {
                    this.setIRQSignal.trigger(0);
                }
            }
            this.previousPPUAddress = address & 0xFFFF;
        }
    }

}
