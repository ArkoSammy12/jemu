package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NES6502;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;

public class NESEmulator implements Emulator, NMOS6502.SystemBus {

    private final SystemHost systemHost;

    private final NES6502 cpu;
    private final NESPPU<?> ppu;
    private final NESAPU<?> apu;
    private final NESController<?> controller;

    public NESEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new NES6502(this);
        this.ppu = new NESPPU<>(this);
        this.apu = new NESAPU<>(this);
        this.controller = new NESController<>(this);
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    @Override
    public Processor getCpu() {
        return this.cpu;
    }

    @Override
    public VideoGenerator<?> getVideoGenerator() {
        return this.ppu;
    }

    @Override
    public AudioGenerator<?> getAudioGenerator() {
        return apu;
    }

    @Override
    public SystemController<?> getSystemController() {
        return this.controller;
    }

    @Override
    public BusView getBusView() {
        return null;
    }

    @Override
    public void executeFrame() {

    }

    @Override
    public void executeCycle() {

    }

    @Override
    public int getFramerate() {
        return 60;
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
        return false;
    }

    @Override
    public boolean getRdy() {
        return false;
    }

    @Override
    public Bus getBus() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
