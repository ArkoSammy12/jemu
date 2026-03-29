package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.cpu.NES6502;
import io.github.arkosammy12.jemu.core.cpu.NMOS6502;

public class NESEmulator implements Emulator, NMOS6502.SystemBus {

    private final SystemHost systemHost;

    private final NES6502 cpu;

    public NESEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new NES6502(this);
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
        return null;
    }

    @Override
    public AudioGenerator<?> getAudioGenerator() {
        return null;
    }

    @Override
    public SystemController<?> getSystemController() {
        return null;
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
