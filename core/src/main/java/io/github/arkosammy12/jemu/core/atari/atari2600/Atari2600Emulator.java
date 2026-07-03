package io.github.arkosammy12.jemu.core.atari.atari2600;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NMOS6507;

public class Atari2600Emulator implements Emulator, NMOS6507.SystemBus {

    private static final int NTSC_CPU_CLOCK_SPEED = 1193182;
    private static final int NTSC_FRAMERATE = 60;

    private static final int PAL_CPU_CLOCK_SPEED = 1182298;
    private static final int PAL_FRAMERATE = 50;

    private final SystemHost systemHost;

    private final NMOS6507 cpu;
    private final TIA<?> tia;
    private final RIOT<?> riot;
    private final Atari2600Bus<?> bus;
    private final Atari2600Controller<?> controller;
    private final Atari2600Cartridge<?> cartridge;

    private final int framerate;
    private final int iterationsPerFrame;

    public Atari2600Emulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new NMOS6507(this);
        this.tia = new TIA<>(this);
        this.riot = new RIOT<>(this);
        this.bus = new Atari2600Bus<>(this);
        this.controller = new Atari2600Controller<>(this);
        this.cartridge = Atari2600Cartridge.getCartridge(this);

        this.framerate = NTSC_FRAMERATE;
        this.iterationsPerFrame = NTSC_CPU_CLOCK_SPEED / this.framerate;
    }

    @Override
    public Atari2600Bus<?> getBus() {
        return this.bus;
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    @Override
    public VideoGenerator getVideoGenerator() {
        return this.tia.getVideo();
    }

    @Override
    public AudioGenerator getAudioGenerator() {
        return this.tia.getAudio();
    }

    @Override
    public SystemController getSystemController() {
        return this.controller;
    }

    public TIA<?> getTIA() {
        return this.tia;
    }

    public RIOT<?> getRIOT() {
        return this.riot;
    }

    public Atari2600Cartridge<?> getCartridge() {
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
        this.cpu.cycle();
        this.cpu.cycle();
        this.tia.cycle();
        this.riot.cycle();
    }

    @Override
    public int getFramerate() {
        return this.framerate;
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
    public boolean getRES() {
        return false;
    }

    @Override
    public boolean getRDY() {
        return this.tia.getRDYSignal();
    }

    @Override
    public void close() throws Exception {

    }

}
