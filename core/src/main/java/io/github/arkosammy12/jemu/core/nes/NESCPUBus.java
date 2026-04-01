package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

public class NESCPUBus<E extends NESEmulator> implements Bus {

    private static final int RAM_START = 0x0000;
    private static final int RAM_END = 0x1FFF;

    public static final int PPU_START = 0x2000;
    public static final int PPU_END = 0x3FFF;

    public static final int APU_IO_START = 0x4000;
    public static final int APU_IO_END = 0x4017;

    public static final int CPU_TEST_MODE_START = 0x4018;
    public static final int CPU_TEST_MODE_END = 0x401F;

    public static final int CARTRIDGE_START = 0x4020;
    public static final int CARTRIDGE_END = 0xFFFF;

    private final E emulator;

    private final int[] ram = new int[0x800];

    public NESCPUBus(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        if (address >= RAM_START && address <= RAM_END) {
            return this.ram[address & 0x7FF];
        } else if (address >= PPU_START && address <= PPU_END) {
            return this.emulator.getMMIOBus().readByte(address);
        } else if (address >= APU_IO_START && address <= APU_IO_END) {
            return this.emulator.getMMIOBus().readByte(address);
        } else if (address >= CPU_TEST_MODE_START && address <= CPU_TEST_MODE_END) {
            return 0xFF;
        } else if (address >= CARTRIDGE_START && address <= CARTRIDGE_END) {
            return this.emulator.getCartridge().readByte(address);
        } else {
            throw new EmulatorException("Invalid NES memory address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        if (address >= RAM_START && address <= RAM_END) {
            this.ram[address & 0x7FF] = value;
        } else if (address >= PPU_START && address <= PPU_END) {
            this.emulator.getMMIOBus().writeByte(address, value);
        } else if (address >= APU_IO_START && address <= APU_IO_END) {
            this.emulator.getMMIOBus().writeByte(address, value);
        } else if (address >= CPU_TEST_MODE_START && address <= CPU_TEST_MODE_END) {

        } else if (address >= CARTRIDGE_START && address <= CARTRIDGE_END) {
            this.emulator.getCartridge().writeByte(address, value);
        } else {
            throw new EmulatorException("Invalid NES memory address $%04X!".formatted(address));
        }
    }

}
