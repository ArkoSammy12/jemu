package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.cpu.NES6502;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.OAMDATA_ADDR;

// TODO: PAL implementation
public class RP2A03<E extends NESEmulator> implements Bus {

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

    private int oamDmaTransferredBytes = 256;
    private int oamDmaSourceAddressHighByte;
    private int oamDmaCurrentData = -1;

    private APUHalfCycleType apuHalfCycleType = APUHalfCycleType.GET;

    private int scheduleDmcDmaHaltCountdown;
    private DmcDmaStep dmcDmaStep = DmcDmaStep.NONE;
    private int dmcDmaAddress;

    public RP2A03(E emulator, int apuSampleBufferSize) {
        this.emulator = emulator;
        this.cpu = new NES6502(emulator);
        this.apu = new NESAPU<>(emulator, apuSampleBufferSize);
        this.controller = new NESController<>(emulator);
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

    public APUHalfCycleType getCurrentApuHalfCycleType() {
        return this.apuHalfCycleType;
    }

    @Override
    public int readByte(int address) {
        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR) {
            return this.apu.readByte(address);
        } else if (address == OAMDMA_ADDR) {
            return -1;
        } else if (address == JOY1_ADDR) {
            return this.controller.readJoy1();
        } else if (address == JOY2_ADDR) {
            return this.controller.readJoy2();
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if ((address >= SQ1_VOL_ADDR && address <= TRI_LINEAR_ADDR) || (address >= TRI_LO_ADDR && address <= NOISE_VOL_ADDR) || (address >= NOISE_LO_ADDR && address <= DMC_LEN_ADDR) || address == SND_CHN_ADDR || address == JOY2_ADDR) {
            this.apu.writeByte(address, value);
        } else if (address == OAMDMA_ADDR) {
            this.oamDmaSourceAddressHighByte = value & 0xFF;
            this.oamDmaTransferredBytes = 0;
        } else if (address == JOY1_ADDR) {
            this.controller.writeJoy1(value);
        }
    }

    public void cycleHalf() {
        boolean isHalted = this.cpu.isHalted();
        NMOS6502.Phase phase = this.cpu.getHalfCyclePhase();
        this.cpu.cycle();
        if (phase == NMOS6502.Phase.PHI_2) {
            this.controller.cycle();

            if (this.scheduleDmcDmaHaltCountdown > 0) {
                this.scheduleDmcDmaHaltCountdown--;
                if (this.scheduleDmcDmaHaltCountdown <= 0) {
                    this.startDmcDma();
                }
            }

            this.apu.cycleHalf();
            this.cycleDma(isHalted);

            this.apuHalfCycleType = this.apuHalfCycleType.getOpposite();
        }
    }

    private void startDmcDma() {
        this.dmcDmaStep = DmcDmaStep.DUMMY;
    }

    private void cycleDma(boolean isHalted) {
        if (!((this.oamDmaTransferredBytes < 256 || this.dmcDmaStep != DmcDmaStep.NONE) && isHalted)) {
            return;
        }
        switch (this.apuHalfCycleType) {
            case GET -> {
                switch (this.dmcDmaStep) {
                    case NONE -> this.tickOamDmaGetIfOngoing();
                    case DUMMY -> {
                        this.dmcDmaStep = DmcDmaStep.GET;
                        this.tickOamDmaGetIfOngoing();
                    }
                    case GET -> {
                        this.apu.writeDmcDma(this.emulator.getBus().readByte(this.dmcDmaAddress));
                        this.dmcDmaStep = DmcDmaStep.NONE;
                    }
                }
            }
            case PUT -> {
                if (this.dmcDmaStep == DmcDmaStep.DUMMY) {
                    this.dmcDmaStep = DmcDmaStep.GET;
                }
                if (this.oamDmaCurrentData >= 0 && this.oamDmaTransferredBytes < 256) {
                    this.emulator.getBus().writeByte(OAMDATA_ADDR, this.oamDmaCurrentData);
                    this.oamDmaTransferredBytes++;
                    this.oamDmaCurrentData = -1;
                } else {
                    this.emulator.getBus().readByte(this.cpu.getLastAddress());
                }
            }
        }
    }

    private void tickOamDmaGetIfOngoing() {
        if (this.oamDmaTransferredBytes < 256) {
            this.oamDmaCurrentData = this.emulator.getBus().readByte((this.oamDmaSourceAddressHighByte << 8) | (this.oamDmaTransferredBytes & 0xFF));
        } else {
            this.emulator.getBus().readByte(this.cpu.getLastAddress());
        }
    }

    void triggerDmcDma(NESAPU.DmcDmaType dmcDmaType, int address) {
        this.dmcDmaAddress = address & 0xFFFF;
        switch (dmcDmaType) {
            case LOAD -> {
                if (this.scheduleDmcDmaHaltCountdown <= 0 && this.dmcDmaStep == DmcDmaStep.NONE) {
                    this.scheduleDmcDmaHaltCountdown = switch (this.apuHalfCycleType) {
                        case GET -> 4;
                        case PUT -> 3;
                    };
                }
            }
            case RELOAD -> {
                if (this.scheduleDmcDmaHaltCountdown <= 0 && this.dmcDmaStep == DmcDmaStep.NONE) {
                    this.scheduleDmcDmaHaltCountdown = switch (this.apuHalfCycleType) {
                        case GET -> 2;
                        case PUT -> 1;
                    };
                }
            }
        }
    }

    public boolean getIRQSignal() {
        return this.apu.getIRQSignal();
    }

    public boolean getRDYSignal() {
        return this.oamDmaTransferredBytes < 256 || this.dmcDmaStep != DmcDmaStep.NONE;
    }

    public enum APUHalfCycleType {
        GET,
        PUT;

        private APUHalfCycleType getOpposite() {
            return switch (this) {
                case GET -> PUT;
                case PUT -> GET;
            };
        }
    }

    private enum DmcDmaStep {
        NONE,
        DUMMY,
        GET
    }

}
