package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import static io.github.arkosammy12.jemu.core.nes.RP2A03.*;

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
    private int dataBus;

    public NESCPUBus(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        if (address == SND_CHN_ADDR) {
            int sndChnByte = this.emulator.getRicohCore().readByte(address);
            if (sndChnByte >= 0) {
                return (sndChnByte & ~0b00100000) | (this.dataBus & 0b00100000);
            } else {
                return this.dataBus;
            }
        }

        int ret = -1;

        if (address >= RAM_START && address <= RAM_END) {
            ret = this.ram[address & 0x7FF];

        }

        int ppuByte = this.emulator.getVideoGenerator().readByte(address);
        if (ppuByte >= 0) {
            ret = ret >= 0 ? ppuByte & ret : ppuByte;
        }

        int apuIoByte = this.emulator.getRicohCore().readByte(address);
        if (apuIoByte >= 0) {
            ret = ret >= 0 ? apuIoByte & ret : apuIoByte;
        }

        int cartridgeByte = this.emulator.getCartridge().readByte(address);
        if (cartridgeByte >= 0) {
            ret = ret >= 0 ? cartridgeByte & ret : cartridgeByte;
        }

        if (ret >= 0) {
            if (address == JOY1_ADDR || address == JOY2_ADDR) {
                ret = (ret & ~0xE0) | (this.dataBus & 0xE0);
            }
            this.dataBus = ret;
            return ret;
        } else {
            return this.dataBus;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        value &= 0xFF;
        this.dataBus = value;
        if (address >= RAM_START && address <= RAM_END) {
            this.ram[address & 0x7FF] = value;
        }

        this.emulator.getVideoGenerator().writeByte(address, value);
        this.emulator.getRicohCore().writeByte(address, value);
        this.emulator.getCartridge().writeByte(address, value);
    }

}
