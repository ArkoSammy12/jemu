package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public class NESEmulator implements Emulator, NMOS6502.SystemBus {

    private static final int FRAMERATE = 60;

    private static final int NTSC_MASTER_CLOCK_FREQUENCY_HZ = 236_250_000 / 11;
    private static final int NTSC_CPU_CLOCK_DIVISOR = 12;
    private static final int NTSC_PPU_CLOCK_DIVISOR = 4;

    private static final int PAL_MASTER_CLOCK_FREQUENCY_HZ = (int) Math.round(26_601_712.5);
    private static final int PAL_CPU_CLOCK_DIVISOR = 16;
    private static final int PAL_PPU_CLOCK_DIVISOR = 5;

    private final SystemHost systemHost;

    private final RP2A03<?> ricohCore;
    private final NESPPU<?> ppu;
    private final NESCPUBus<?> cpuBus;
    private final NESCartridge<?> cartridge;

    private final int iterationsPerFrame;
    private final int ppuDivisor;

    private int ppuDivisorCounter;

    public NESEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;

        this.ricohCore = new RP2A03<>(this, 0.5, NTSC_MASTER_CLOCK_FREQUENCY_HZ / NTSC_CPU_CLOCK_DIVISOR / FRAMERATE);
        this.ppu = new NESPPU<>(this);
        this.cpuBus = new NESCPUBus<>(this);
        this.cartridge = NESCartridge.getCartridge(this, INESFile.getINESFile(SystemHost.byteToIntArray(this.getHost().getRom())));

        // TODO: PAL support
        this.iterationsPerFrame = (NTSC_MASTER_CLOCK_FREQUENCY_HZ / FRAMERATE);
        this.ppuDivisor = NTSC_PPU_CLOCK_DIVISOR / 2;
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
    public NESPPU<?> getVideoGenerator() {
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
        for (int i = 0; i < this.iterationsPerFrame; i++) {
            this.runCycle();
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
    }

    private void runCycle() {
        this.ricohCore.onMasterClock();

        this.ppuDivisorCounter--;
        if (this.ppuDivisorCounter <= 0) {
            this.ppu.cycleHalfDot();
            this.ppuDivisorCounter = this.ppuDivisor;
        }
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
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
