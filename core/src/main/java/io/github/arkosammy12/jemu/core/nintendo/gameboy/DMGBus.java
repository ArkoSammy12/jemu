package io.github.arkosammy12.jemu.core.nintendo.gameboy;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.common.Bus;

import java.util.function.IntUnaryOperator;

import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGAPU.*;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGPPU.*;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGSerialController.SB_ADDR;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGSerialController.SC_ADDR;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.DMGTimerController.*;
import static io.github.arkosammy12.jemu.core.nintendo.gameboy.GameBoyJoypad.JOYP_ADDR;

public class DMGBus<E extends GameBoyEmulator> implements Bus {

    // Bootix boot-rom for the DMG. Courtesy of Ashiepaws https://github.com/Ashiepaws/Bootix
    protected static final int[] BOOTIX = {
            0x31, 0xfe, 0xff, 0x21, 0xff, 0x9f, 0xaf, 0x32, 0xcb, 0x7c, 0x20, 0xfa,
            0x0e, 0x11, 0x21, 0x26, 0xff, 0x3e, 0x80, 0x32, 0xe2, 0x0c, 0x3e, 0xf3,
            0x32, 0xe2, 0x0c, 0x3e, 0x77, 0x32, 0xe2, 0x11, 0x04, 0x01, 0x21, 0x10,
            0x80, 0x1a, 0xcd, 0xb8, 0x00, 0x1a, 0xcb, 0x37, 0xcd, 0xb8, 0x00, 0x13,
            0x7b, 0xfe, 0x34, 0x20, 0xf0, 0x11, 0xcc, 0x00, 0x06, 0x08, 0x1a, 0x13,
            0x22, 0x23, 0x05, 0x20, 0xf9, 0x21, 0x04, 0x99, 0x01, 0x0c, 0x01, 0xcd,
            0xb1, 0x00, 0x3e, 0x19, 0x77, 0x21, 0x24, 0x99, 0x0e, 0x0c, 0xcd, 0xb1,
            0x00, 0x3e, 0x91, 0xe0, 0x40, 0x06, 0x10, 0x11, 0xd4, 0x00, 0x78, 0xe0,
            0x43, 0x05, 0x7b, 0xfe, 0xd8, 0x28, 0x04, 0x1a, 0xe0, 0x47, 0x13, 0x0e,
            0x1c, 0xcd, 0xa7, 0x00, 0xaf, 0x90, 0xe0, 0x43, 0x05, 0x0e, 0x1c, 0xcd,
            0xa7, 0x00, 0xaf, 0xb0, 0x20, 0xe0, 0xe0, 0x43, 0x3e, 0x83, 0xcd, 0x9f,
            0x00, 0x0e, 0x27, 0xcd, 0xa7, 0x00, 0x3e, 0xc1, 0xcd, 0x9f, 0x00, 0x11,
            0x8a, 0x01, 0xf0, 0x44, 0xfe, 0x90, 0x20, 0xfa, 0x1b, 0x7a, 0xb3, 0x20,
            0xf5, 0x18, 0x49, 0x0e, 0x13, 0xe2, 0x0c, 0x3e, 0x87, 0xe2, 0xc9, 0xf0,
            0x44, 0xfe, 0x90, 0x20, 0xfa, 0x0d, 0x20, 0xf7, 0xc9, 0x78, 0x22, 0x04,
            0x0d, 0x20, 0xfa, 0xc9, 0x47, 0x0e, 0x04, 0xaf, 0xc5, 0xcb, 0x10, 0x17,
            0xc1, 0xcb, 0x10, 0x17, 0x0d, 0x20, 0xf5, 0x22, 0x23, 0x22, 0x23, 0xc9,
            0x3c, 0x42, 0xb9, 0xa5, 0xb9, 0xa5, 0x42, 0x3c, 0x00, 0x54, 0xa8, 0xfc,
            0x42, 0x4f, 0x4f, 0x54, 0x49, 0x58, 0x2e, 0x44, 0x4d, 0x47, 0x20, 0x76,
            0x31, 0x2e, 0x32, 0x00, 0x3e, 0xff, 0xc6, 0x01, 0x0b, 0x1e, 0xd8, 0x21,
            0x4d, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x3e, 0x01, 0xe0, 0x50
    };

    public static final int ROM0_START = 0x0000;
    public static final int ROMX_END = 0x7FFF;

    public static final int VRAM_START = 0x8000;
    public static final int VRAM_END = 0x9FFF;

    public static final int SRAM_START = 0xA000;
    public static final int SRAM_END = 0xBFFF;

    public static final int WRAM0_START = 0xC000;
    public static final int WRAMX_END = 0xDFFF;
    public static final int ECHO_END = 0xFDFF;

    public static final int OAM_START = 0xFE00;
    public static final int OAM_END = 0xFE9F;

    public static final int UNUSED_START = 0xFEA0;
    public static final int UNUSED_END = 0xFEFF;

    public static final int IO_START = 0xFF00;
    public static final int IO_END = 0xFF7F;

    public static final int HRAM_START = 0xFF80;
    public static final int HRAM_END = 0xFFFE;

    public static final int IF_ADDR = 0xFF0F;
    public static final int OAM_DMA_ADDR = 0xFF46;
    public static final int BANK_ADDR = 0xFF50;
    public static final int IE_ADDR = 0xFFFF;

    protected final E emulator;
    private final IntUnaryOperator bootRomDisabledReadCartridgeReadFunction;
    protected IntUnaryOperator currentReadCartridgeFunction;

    protected final byte[] workRAM;

    private int interruptFlag;
    private int interruptEnable;

    private int oamDmaControl;
    private int oamDmaStartDelay;
    protected boolean oamDmaInProgress;
    private int oamDmaTransferredBytes;
    private int oamDmaByte = 0xFF;

    protected boolean enableBootRom = true;

    public DMGBus(E emulator) {
        this.emulator = emulator;
        this.workRAM = this.createWorkRAM();
        this.bootRomDisabledReadCartridgeReadFunction = address -> this.emulator.getCartridge().readByte(address);
        this.currentReadCartridgeFunction = this.getBootRomEnabledCartridgeReadFunction();
    }

    protected byte[] createWorkRAM() {
        return new byte[0x2000];
    }

    protected IntUnaryOperator getBootRomEnabledCartridgeReadFunction() {
        return address -> {
            if (address <= 0x00FF) {
                return BOOTIX[address];
            } else {
                return this.emulator.getCartridge().readByte(address);
            }
        };
    }

    public boolean isBootRomEnabled() {
        return this.enableBootRom;
    }

    public int getIE() {
        return this.interruptEnable;
    }

    public void setIF(int value) {
        this.interruptFlag = value & 0xFF;
    }

    public int getIF() {
        return interruptFlag;
    }

    @Override
    public int readByte(int address) {
        // The address is on the bus from the start of the M-cycle, before this
        // M-cycle's dots run
        this.emulator.getVideoGenerator().checkArmOAMBugRead(address);
        this.emulator.syncPPUForCPURead();

        return switch (this.getOAMDmaBusConflictForCPURead(address)) {
            case OAM -> 0xFF;
            case NORMAL -> this.oamDmaByte;
            case NONE -> {
                if ((address >= ROM0_START && address <= ROMX_END) || (address >= SRAM_START && address <= SRAM_END)) {
                    yield this.currentReadCartridgeFunction.applyAsInt(address);
                } else if ((address >= VRAM_START && address <= VRAM_END) || (address >= OAM_START && address <= OAM_END)) {
                    yield this.emulator.getVideoGenerator().readByte(address);
                } else if (address >= WRAM0_START && address <= ECHO_END) {
                    yield this.readWorkRAM(address);
                } else if (address >= UNUSED_START && address <= UNUSED_END) {
                    yield 0x00;
                } else if ((address >= IO_START && address <= IO_END) || address == IE_ADDR) {
                    yield this.readByteIO(address);
                } else if (address >= HRAM_START && address <= HRAM_END) {
                    yield this.emulator.getCpu().readHRAM(address);
                } else {
                    throw new EmulatorException("Invalid GameBoy memory address $%04X!".formatted(address));
                }
            }
        };
    }

    protected int readWorkRAM(int address) {
        return (int) this.workRAM[address & 0x1FFF] & 0xFF;
    }

    protected int readByteIO(int address) {
        return switch (address) {
            case OAM_DMA_ADDR -> this.oamDmaControl;
            case BANK_ADDR -> 0xFF;
            case JOYP_ADDR -> this.emulator.getSystemController().readJoypad();
            case SB_ADDR, SC_ADDR -> this.emulator.getSerialController().readByte(address);
            case DIV_ADDR, TIMA_ADDR, TMA_ADDR, TAC_ADDR -> this.emulator.getTimerController().readByte(address);
            case IF_ADDR -> this.interruptFlag | 0b11100000;
            case IE_ADDR -> this.interruptEnable;
            default -> {
                if ((address >= NR10_ADDR && address <= NR14_ADDR) || (address >= NR21_ADDR && address <= NR34_ADDR) || (address >= NR41_ADDR && address <= NR52_ADDR) || (address >= WAVERAM_START && address <= WAVERAM_END)) {
                    yield this.emulator.getAudioGenerator().readByte(address);
                } else if (this.isPPURegisterAddress(address)) {
                    yield this.emulator.getVideoGenerator().readByte(address);
                } else {
                    yield 0xFF;
                }
            }
        };
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;

        this.emulator.getVideoGenerator().checkArmOAMBugWrite(address);
        if (this.isPPURegisterAddress(address)) {
            this.emulator.syncPPUForCPUPPURegisterWrite();
        }

        if (this.oamDmaInProgress && address < 0xFF00) {
            return;
        }

        if ((address >= ROM0_START && address <= ROMX_END) || (address >= SRAM_START && address <= SRAM_END)) {
            this.emulator.getCartridge().writeByte(address, value);
        } else if ((address >= VRAM_START && address <= VRAM_END) || (address >= OAM_START && address <= OAM_END)) {
            this.emulator.getVideoGenerator().writeByte(address, value);
        } else if (address >= WRAM0_START && address <= ECHO_END) {
            this.writeWorkRAM(address, value);
        } else if (address >= UNUSED_START && address <= UNUSED_END) {

        } else if ((address >= IO_START && address <= IO_END) || address == IE_ADDR) {
            this.writeByteIO(address, value);
        } else if (address >= HRAM_START && address <= HRAM_END) {
            this.emulator.getCpu().writeHRAM(address, value);
        } else {
            throw new EmulatorException("Invalid GameBoy memory address $%04X!".formatted(address));
        }
    }

    protected void writeWorkRAM(int address, int value) {
        this.workRAM[address & 0x1FFF] = (byte) value;
    }

    protected void writeByteIO(int address, int value) {
        switch (address) {
            case OAM_DMA_ADDR -> {
                this.oamDmaControl = value & 0xFF;
                this.oamDmaStartDelay = 2;
            }
            case BANK_ADDR -> {
                this.enableBootRom = false;
                this.currentReadCartridgeFunction = this.bootRomDisabledReadCartridgeReadFunction;
            }
            case JOYP_ADDR -> this.emulator.getSystemController().writeJoyP(value);
            case SB_ADDR, SC_ADDR -> this.emulator.getSerialController().writeByte(address, value);
            case DIV_ADDR, TIMA_ADDR, TMA_ADDR, TAC_ADDR -> this.emulator.getTimerController().writeByte(address, value);
            case IF_ADDR -> this.interruptFlag = value & 0xFF; // TODO: If the CPU clears a bit on the same cycle that a component tries setting the bit, the bit says cleared!
            case IE_ADDR -> this.interruptEnable = value & 0xFF;
            default -> {
                if ((address >= NR10_ADDR && address <= NR14_ADDR) || (address >= NR21_ADDR && address <= NR34_ADDR) || (address >= NR41_ADDR && address <= NR52_ADDR) || (address >= WAVERAM_START && address <= WAVERAM_END)) {
                    this.emulator.getAudioGenerator().writeByte(address, value);
                } else if (this.isPPURegisterAddress(address)) {
                    this.emulator.getVideoGenerator().writeByte(address, value);
                }
            }
        }
    }

    protected boolean isPPURegisterAddress(int address) {
        return (address >= LCDC_ADDR && address <= LYC_ADDR) || (address >= BGP_ADDR && address <= WX_ADDR);
    }

    public void cycleOAMDMA() {
        if (this.oamDmaInProgress) {
            int sourceAddress = (this.oamDmaControl << 8) | (this.oamDmaTransferredBytes);
            this.oamDmaByte = this.readByteOAMDMA(sourceAddress);
            this.emulator.getVideoGenerator().writeOAMDMA(0xFE00 | this.oamDmaTransferredBytes, this.oamDmaByte);
            this.oamDmaTransferredBytes++;
            if (this.oamDmaTransferredBytes > 0x9F) {
                this.oamDmaByte = 0xFF;
                this.oamDmaInProgress = false;
            }
        }

        if (this.oamDmaStartDelay > 0) {
            this.oamDmaStartDelay--;
            if (this.oamDmaStartDelay <= 0) {
                this.oamDmaInProgress = true;
                this.oamDmaTransferredBytes = 0;
            }
        }
    }

    private int readByteOAMDMA(int address) {
        if ((address >= ROM0_START && address <= ROMX_END) || (address >= SRAM_START && address <= SRAM_END)) {
            return this.currentReadCartridgeFunction.applyAsInt(address);
        } else if (address >= VRAM_START && address <= VRAM_END) {
            return this.emulator.getVideoGenerator().readByte(address);
        } else if (address >= WRAM0_START && address <= 0xFFFF) {
            return this.readWorkRAM(address);
        } else {
            throw new EmulatorException("Invalid GameBoy memory address %04X!".formatted(address));
        }
    }

    private OAMDMABusConflict getOAMDmaBusConflictForCPURead(int address) {
        if (!this.oamDmaInProgress) {
            return OAMDMABusConflict.NONE;
        }
        if (address >= HRAM_START && address <= HRAM_END) {
            return OAMDMABusConflict.NONE;
        }
        if (address >= OAM_START && address <= OAM_END) {
            return OAMDMABusConflict.OAM;
        }
        return this.areUsingSameBuses(address, (this.oamDmaControl << 8) | (this.oamDmaTransferredBytes)) ? OAMDMABusConflict.NORMAL : OAMDMABusConflict.NONE;
    }

    protected boolean areUsingSameBuses(int address1, int address2) {
        return (isExternalBus(address1) && isExternalBus(address2)) || (isVRAMBus(address1) && isVRAMBus(address2));
    }

    private static boolean isExternalBus(int address) {
        return (address >= 0x0000 && address <= 0x7FFF) || (address >= 0xA000 && address <= 0xBFFF) || (address >= WRAM0_START && address <= ECHO_END);
    }

    protected static boolean isVRAMBus(int address) {
        return address >= VRAM_START && address <= VRAM_END;
    }

    protected enum OAMDMABusConflict {
        NONE,
        NORMAL,
        OAM,
    }

}
