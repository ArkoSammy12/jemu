package io.github.arkosammy12.jemu.systems.bus;

import io.github.arkosammy12.jemu.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.systems.GameBoyEmulator;

public class GameBoyBus implements Bus, BusView {

    public static final int ROM0_START = 0x0000;
    public static final int ROM0_END = 0x3FFF;

    public static final int ROMX_START = 0x4000;
    public static final int ROMX_END = 0x7FFF;

    public static final int VRAM_START = 0x8000;
    public static final int VRAM_END = 0x9FFF;

    public static final int SRAM_START = 0xA000;
    public static final int SRAM_END = 0xBFFF;

    public static final int WRAM0_START = 0xC000;
    public static final int WRAM0_END = 0xCFFF;

    public static final int WRAMX_START = 0xD000;
    public static final int WRAMX_END = 0xDFFF;

    public static final int ECHO_START = 0xE000;
    public static final int ECHO_END = 0xFDFF;

    public static final int OAM_START = 0xFE00;
    public static final int OAM_END = 0xFE9F;

    public static final int UNUSED_START = 0xFEA0;
    public static final int UNUSED_END = 0xFEFF;

    public static final int IO_START = 0xFF00;
    public static final int IO_END = 0xFF7F;

    public static final int HRAM_START = 0xFF80;
    public static final int HRAM_END = 0xFFFE;

    public static final int IE_REGISTER = 0xFFFF;

    private final GameBoyEmulator emulator;

    private final int[] vRam = new int[0x2000];
    private final int[] wRam = new int[0x2000];
    private final int[] oam = new int[0x00A0];

    public GameBoyBus(GameBoyEmulator emulator) {
        this.emulator = emulator;
    }

    @Override
    public int getMemorySize() {
        return 0;
    }

    @Override
    public int getMemoryBoundsMask() {
        return 0;
    }

    @Override
    public int getByte(int address) {
        return 0;
    }

    // TODO: DMA access sees repeated ECHO starting from 0xFE00

    @Override
    public int readByte(int address) {
        if (address >= ROM0_START && address <= ROM0_END) {
            return this.emulator.getCartridge().readByte(address);
        } else if (address >= ROMX_START && address <= ROMX_END) {
            return this.emulator.getCartridge().readByte(address);
        } else if (address >= VRAM_START && address <= VRAM_END) {
            return this.vRam[address - VRAM_START];
        } else if (address >= SRAM_START && address <= SRAM_END) {
            return this.emulator.getCartridge().readByte(address);
        } else if (address >= WRAM0_START && address <= WRAM0_END) {
            return this.wRam[address - WRAM0_START];
        } else if (address >= WRAMX_START && address <= WRAMX_END) {
            return this.wRam[address - WRAM0_START];
        } else if (address >= ECHO_START && address <= ECHO_END) {
            return this.wRam[address & 0x1FFF];
        } else if (address >= OAM_START && address <= OAM_END) {
            return this.oam[address - OAM_START];
        } else if (address >= UNUSED_START && address <= UNUSED_END) {
            // TODO: IMPLEMENT OAM LOCKING/UNLOCKING. RETURN 0 WHEN UNLOCKED, 0xFF WHEN LOCKED
            return 0;
        } else if (address >= IO_START && address <= IO_END) {
            return this.emulator.getMMIOController().readByte(address);
        } else if (address >= HRAM_START && address <= HRAM_END) {
            return this.emulator.getProcessor().readHRam(address - HRAM_START);
        } else if (address == IE_REGISTER) {
            return this.emulator.getMMIOController().readByte(address);
        } else {
            throw new EmulatorException("Invalid address: " + String.format("%04X", address) + " for the GameBoy system!");
        }
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        if (address >= ROM0_START && address <= ROM0_END) {
            this.emulator.getCartridge().writeByte(address, value);
        } else if (address >= ROMX_START && address <= ROMX_END) {
            this.emulator.getCartridge().writeByte(address, value);
        } else if (address >= VRAM_START && address <= VRAM_END) {
            this.vRam[address - VRAM_START] = value;
        } else if (address >= SRAM_START && address <= SRAM_END) {
            this.emulator.getCartridge().writeByte(address, value);
        } else if (address >= WRAM0_START && address <= WRAM0_END) {
            this.wRam[address - WRAM0_START] = value;
        } else if (address >= WRAMX_START && address <= WRAMX_END) {
            this.wRam[address - WRAM0_START] = value;
        } else if (address >= ECHO_START && address <= ECHO_END) {
            this.wRam[address & 0x1FFF] = value;
        } else if (address >= OAM_START && address <= OAM_END) {
            this.oam[address - OAM_START] = value;
        } else if (address >= UNUSED_START && address <= UNUSED_END) {
            // TODO: TRIGGER OAM BUG
        } else if (address >= IO_START && address <= IO_END) {
            this.emulator.getMMIOController().writeByte(address, value);
        } else if (address >= HRAM_START && address <= HRAM_END) {
            this.emulator.getProcessor().writeHRam(address - HRAM_START, value);
        } else if (address == IE_REGISTER) {
            this.emulator.getMMIOController().writeByte(address, value);
        } else {
            throw new EmulatorException("Invalid address: " + String.format("%04X", address) + " for the GameBoy system!");
        }
    }

}
