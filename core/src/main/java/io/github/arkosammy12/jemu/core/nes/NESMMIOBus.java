package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;

import static io.github.arkosammy12.jemu.core.nes.NESCPUBus.*;

public class NESMMIOBus<E extends NESEmulator> implements Bus {

    public static final int PPUCTRL_ADDR = 0x2000;
    public static final int PPUMASK_ADDR = 0x2001;
    public static final int PPUSTATUS_ADDR = 0x2002;
    public static final int OAMADDR_ADDR = 0x2003;
    public static final int OAMDATA_ADDR = 0x2004;
    public static final int PPUSCROLL_ADDR = 0x2005;
    public static final int PPUADDR_ADDR = 0x2006;
    public static final int PPUDATA_ADDR = 0x2007;

    public static final int SQ1_VOL_ADDR = 0x4000;
    public static final int SQ1_SWEEP_ADDR = 0x4001;
    public static final int SQ1_LO_ADDR = 0x4002;
    public static final int SQ1_HI_ADDR = 0x4003;
    public static final int SQ2_VOL_ADDR = 0x4004;
    public static final int SQ2_SWEEP_ADDR = 0x4005;
    public static final int SQ2_LO_ADDR = 0x4006;
    public static final int SQ2_HI_ADDR = 0x4007;
    public static final int TRI_LINEAR_ADDR = 0x4008;

    public static final int TRI_LO_ADDR = 0x400A;
    public static final int TRI_HI_ADDR = 0x400B;
    public static final int NOISE_VOL_ADDR = 0x400C;

    public static final int NOISE_LO_ADDR = 0x400E;
    public static final int NOISE_HI_ADDR = 0x400F;
    public static final int DMC_FREQ_ADDR = 0x4010;
    public static final int DMC_RAW_ADDR = 0x4011;
    public static final int DMC_START_ADDR = 0x4012;
    public static final int DMC_LEN_ADDR = 0x4013;

    public static final int OAMDMA = 0x4014;

    public static final int SND_CHN = 0x4015;

    public static final int JOY1 = 0x4016;

    public static final int JOY2 = 0x4017;

    private final E emulator;

    public NESMMIOBus(E emulator) {
        this.emulator = emulator;
    }

    @Override
    public int readByte(int address) {
        if (address >= PPU_START && address <= PPU_END) {
            address = 0x2000 + (address & 7);
            return this.emulator.getVideoGenerator().readByte(address);
        } else if ((address >= SQ1_VOL_ADDR && address <= 0x4008) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN) {
            return this.emulator.getAudioGenerator().readByte(address);
        } else if (address == OAMDMA) {
            return 0xFF;
        } else if (address == JOY1) {
            return this.emulator.getSystemController().readJoy1();
        } else if (address == JOY2) {
            return this.emulator.getSystemController().readJoy2();
        } else {
            return 0xFF;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= PPU_START && address <= PPU_END) {
            address = 0x2000 + (address & 7);
            this.emulator.getVideoGenerator().writeByte(address, value);
        } else if ((address >= SQ1_VOL_ADDR && address <= 0x4008) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN || address == JOY2) {
            this.emulator.getAudioGenerator().writeByte(address, value);
        } else if (address == OAMDMA) {

        } else if (address == JOY1) {
            this.emulator.getSystemController().writeJoy1(value);
        }
    }
}
