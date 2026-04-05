package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.NES6502;

import static io.github.arkosammy12.jemu.core.nes.NESPPU.OAMDATA_ADDR;

public class RP2A03<E extends NESEmulator> implements Bus {

    private static final int NTSC_CPU_CLOCK_DIVISOR = 12;

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

    public static final int OAMDMA_ADDR = 0x4014;
    public static final int SND_CHN_ADDR = 0x4015;
    public static final int JOY1_ADDR = 0x4016;
    public static final int JOY2_ADDR = 0x4017;

    private final E emulator;
    private final NES6502 cpu;
    private final NESAPU<?> apu;
    private final NESController<?> controller;

    private final int cpuDivisor;
    private final int apuDivisor;

    private int cpuDivisorCounter;
    private int apuDivisorCounter;

    private int oamDmaTransferredBytes = 256;
    private int oamDmaSourceAddressHighByte;
    private int oamDmaCurrentData = -1;
    private boolean rdySignal;

    private APUHalfCycleType apuHalfCycleType = APUHalfCycleType.GET;

    public RP2A03(E emulator, double baseClockDividerMultipliers) {
        this.emulator = emulator;
        this.cpuDivisor = (int) (NTSC_CPU_CLOCK_DIVISOR * baseClockDividerMultipliers);
        this.apuDivisor = this.cpuDivisor * 2;

        this.cpu = new NES6502(emulator);
        this.apu = new NESAPU<>(emulator);
        this.controller = new NESController<>(emulator);
    }

    public void onMasterClock() {
        this.cpuDivisorCounter--;
        if (this.cpuDivisorCounter <= 0) {
            this.cpu.cycle();
            this.cpuDivisorCounter = this.cpuDivisor;
        }

        this.apuDivisorCounter--;
        if (this.apuDivisorCounter <= 0) {
            this.cycleDma();
            this.apuHalfCycleType = this.apuHalfCycleType.getOpposite();
            this.apuDivisorCounter = this.apuDivisor;
        }
    }

    private void cycleDma() {
        if (this.oamDmaTransferredBytes >= 256 || !this.cpu.isHalted()) {
            return;
        }
        switch (this.apuHalfCycleType) {
            case GET -> {
                this.oamDmaCurrentData = this.emulator.getBus().readByte((this.oamDmaSourceAddressHighByte << 8) | (this.oamDmaTransferredBytes & 0xFF));
            }
            case PUT -> {
                if (this.oamDmaCurrentData >= 0) {
                    this.emulator.getBus().writeByte(OAMDATA_ADDR, this.oamDmaCurrentData);
                    this.oamDmaTransferredBytes++;
                    if (this.oamDmaTransferredBytes >= 256) {
                        this.oamDmaCurrentData = -1;
                        this.rdySignal = false;
                    }
                }
            }
        }
    }

    public boolean getIRQSignal() {
        return false;
    }

    public boolean getRDYSignal() {
        return this.rdySignal;
    }

    public NES6502 getCpu() {
        return this.cpu;
    }

    public NESAPU<?> getApu() {
        return this.apu;
    }

    public NESController<?> getController() {
        return this.controller;
    }

    @Override
    public int readByte(int address) {
        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR) {
            return this.apu.readByte(address);
        } else if (address == OAMDMA_ADDR) {
            return 0xFF;
        } else if (address == JOY1_ADDR) {
            return this.controller.readJoy1();
        } else if (address == JOY2_ADDR) {
            return this.controller.readJoy2();
        } else {
            return 0xFF;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR || address == JOY2_ADDR) {
            this.apu.writeByte(address, value);
        } else if (address == OAMDMA_ADDR) {
            this.oamDmaSourceAddressHighByte = value & 0xFF;
            this.rdySignal = true;
        } else if (address == JOY1_ADDR) {
            this.controller.writeJoy1(value);
        }
    }

    private enum APUHalfCycleType {
        GET,
        PUT;

        private APUHalfCycleType getOpposite() {
            return switch (this) {
                case GET -> PUT;
                case PUT -> GET;
            };
        }
    }

}
