package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public class NESEmulator implements Emulator, NMOS6502.SystemBus {

    private static final int NTSC_MASTER_CLOCK_FREQUENCY_HZ = 236_250_000 / 11;
    private static final int NTSC_CPU_CLOCK_DIVISOR = 12;
    private static final int NTSC_PPU_CLOCK_DIVISOR = 4;
    private static final int NTSC_FRAMERATE = 60;

    private static final int PAL_MASTER_CLOCK_FREQUENCY_HZ = (int) Math.round(26_601_712.5);
    private static final int PAL_CPU_CLOCK_DIVISOR = 16;
    private static final int PAL_PPU_CLOCK_DIVISOR = 5;
    private static final int PAL_FRAMERATE = 50;

    private final SystemHost systemHost;

    private final RP2A03<?> ricohCore;
    private final RP2C02<?> ppu;
    private final NESCPUBus<?> cpuBus;
    private final NESCartridge<?> cartridge;

    private final boolean isPAL;
    private final int iterationsPerFrame;
    private final boolean deriveCyclesFromMasterClock;

    private final int masterClockFrequency;
    private final int framerate;

    private final int cpuSubCycleDivisor;
    private int cpuDivisorCounter;

    private final int ppuSubCycleDivisor;
    private int ppuDivisorCounter;

    public NESEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cartridge = NESCartridge.getCartridge(this, INESFile.getINESFile(SystemHost.byteToIntArray(this.getHost().getRom())));
        this.isPAL = this.cartridge.getINESFile().isPAL();
        int apuSampleBufferSize;
        if (this.isPAL) {
            this.masterClockFrequency = PAL_MASTER_CLOCK_FREQUENCY_HZ;
            this.framerate = PAL_FRAMERATE;
            this.iterationsPerFrame = (PAL_MASTER_CLOCK_FREQUENCY_HZ * 2) / PAL_FRAMERATE;
            this.cpuSubCycleDivisor = PAL_CPU_CLOCK_DIVISOR;
            this.ppuSubCycleDivisor = PAL_PPU_CLOCK_DIVISOR;
            apuSampleBufferSize = (PAL_MASTER_CLOCK_FREQUENCY_HZ * 2) / PAL_CPU_CLOCK_DIVISOR / PAL_FRAMERATE;
            this.deriveCyclesFromMasterClock = true;
        } else {
            this.masterClockFrequency = NTSC_MASTER_CLOCK_FREQUENCY_HZ;
            this.framerate = NTSC_FRAMERATE;
            this.iterationsPerFrame = NTSC_MASTER_CLOCK_FREQUENCY_HZ / NTSC_CPU_CLOCK_DIVISOR / NTSC_FRAMERATE;
            this.cpuSubCycleDivisor = NTSC_CPU_CLOCK_DIVISOR / 2;
            this.ppuSubCycleDivisor = NTSC_PPU_CLOCK_DIVISOR / 2;
            apuSampleBufferSize = this.iterationsPerFrame;
            this.deriveCyclesFromMasterClock = false;
        }

        this.ricohCore = new RP2A03<>(this, apuSampleBufferSize);
        this.ppu = new RP2C02<>(this);
        this.cpuBus = new NESCPUBus<>(this);
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    public RP2A03<?> getRicohCore() {
        return this.ricohCore;
    }

    public Processor getCpu() {
        return this.ricohCore.getCpu();
    }

    @Override
    public RP2C02<?> getVideoGenerator() {
        return this.ppu;
    }

    @Override
    public NESAPU<?> getAudioGenerator() {
        return this.ricohCore.getApu();
    }

    @Override
    public NESController<?> getSystemController() {
        return this.ricohCore.getController();
    }

    @Override
    public NESCPUBus<?> getBus() {
        return this.cpuBus;
    }

    public NESCartridge<?> getCartridge() {
        return this.cartridge;
    }

    @Override
    public void executeFrame() {
        if (this.deriveCyclesFromMasterClock) {
            for (int i = 0; i < this.iterationsPerFrame; i++) {
                this.runCycleWithClockDivisors();
            }
        } else {
            for (int i = 0; i < this.iterationsPerFrame; i++) {
                this.runCycleWithRatio();
            }
        }
    }

    @Override
    public void executeCycle() {
        if (this.deriveCyclesFromMasterClock) {
            this.runCycleWithClockDivisors();
        } else {
            this.runCycleWithRatio();
        }
    }

    private void runCycleWithRatio() {
        this.ricohCore.cycleHalf();
        this.ppu.cycleHalfDot();
        this.ppu.cycleHalfDot();
        this.ppu.cycleHalfDot();

        this.ricohCore.cycleHalf();
        this.ppu.cycleHalfDot();
        this.ppu.cycleHalfDot();
        this.ppu.cycleHalfDot();
    }

    private void runCycleWithClockDivisors() {
        this.cpuDivisorCounter--;
        if (this.cpuDivisorCounter <= 0) {
            this.ricohCore.cycleHalf();
            this.cpuDivisorCounter = this.cpuSubCycleDivisor;
        }

        this.ppuDivisorCounter--;
        if (this.ppuDivisorCounter <= 0) {
            this.ppu.cycleHalfDot();
            this.ppuDivisorCounter = this.ppuSubCycleDivisor;
        }
    }

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getIRQ() {
        return this.ricohCore.getIRQSignal() || this.cartridge.getIRQSignal();
    }

    @Override
    public boolean getNMI() {
        return this.ppu.getNMISignal();
    }

    @Override
    public boolean getRES() {
        return false;
    }

    @Override
    public boolean getRDY() {
        return this.ricohCore.getRDYSignal();
    }

    @Override
    public void close() throws Exception {

    }

}
