package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NES6502;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

public class NESEmulator implements Emulator, NMOS6502.SystemBus {

    private static final int FRAMERATE = 60;

    private static final int CPU_CLOCK_SPEED_HZ = 1789773;
    private static final int CPU_CYCLES_PER_FRAME = CPU_CLOCK_SPEED_HZ / FRAMERATE;
    private static final int CPU_SUB_CYCLES_PER_FRAME = CPU_CYCLES_PER_FRAME * 2;

    private final SystemHost systemHost;
    private final INESFile iNESFile;

    private final NES6502 cpu;
    private final NESPPU<?> ppu;
    private final NESAPU<?> apu;
    private final NESController<?> controller;
    private final NESCPUBus<?> cpuBus;
    private final NESMMIOBus<?> mmioBus;
    private final NESCartridge cartridge;

    private boolean initialReset = true;
    private boolean initialResetUnset = false;
    private int initialResetUnsetCountdown = 10;

    public NESEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new NES6502(this);
        this.ppu = new NESPPU<>(this);
        this.apu = new NESAPU<>(this);
        this.controller = new NESController<>(this);

        this.cpuBus = new NESCPUBus<>(this);
        this.mmioBus = new NESMMIOBus<>(this);

        this.iNESFile = INESFile.getINESFile(SystemHost.byteToIntArray(this.getHost().getRom()));
        this.cartridge = NESCartridge.getCartridge(this);
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    public INESFile getiNESFile() {
        return this.iNESFile;
    }

    public Processor getCpu() {
        return this.cpu;
    }

    @Override
    public NESPPU<?> getVideoGenerator() {
        return this.ppu;
    }

    @Override
    public NESAPU<?> getAudioGenerator() {
        return apu;
    }

    @Override
    public NESController<?> getSystemController() {
        return this.controller;
    }

    @Override
    public NESCPUBus<?> getBus() {
        return this.cpuBus;
    }

    public NESMMIOBus<?> getMMIOBus() {
        return this.mmioBus;
    }

    public NESCartridge getCartridge() {
        return this.cartridge;
    }

    @Override
    public void executeFrame() {
        if (this.initialResetUnset) {
            for (int i = 0; i < CPU_SUB_CYCLES_PER_FRAME; i++) {
                this.runCycle();
            }
        } else {
            for (int i = 0; i < CPU_SUB_CYCLES_PER_FRAME; i++) {
                this.runCycle();
                this.autoUnsetInitialResetIfNecessary();
            }
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
        this.autoUnsetInitialResetIfNecessary();
    }

    private void autoUnsetInitialResetIfNecessary() {
        this.initialResetUnsetCountdown--;
        if (!this.initialResetUnset && this.initialResetUnsetCountdown <= 0) {
            this.initialReset = false;
            this.initialResetUnset = true;
        }
    }

    private void runCycle() {
        this.cpu.cycle();
    }

    @Override
    public int getFramerate() {
        return FRAMERATE;
    }

    @Override
    public boolean getIRQ() {
        return false;
    }

    @Override
    public boolean getNMI() {
        return false;
    }

    @Override
    public boolean getRes() {
        return this.initialReset;
    }

    @Override
    public boolean getRdy() {
        return false;
    }

    @Override
    public void close() throws Exception {

    }

}
