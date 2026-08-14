package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.hardware.NMOS6510;

public class Commodore64Emulator implements Emulator, NMOS6510.SystemBus {

    private static final int CPU_CLOCK_DIVISOR = 8;

    private static final int PAL_PHI_IN_HZ = 7_881_990;
    private static final int PAL_FRAMERATE = 50;

    private final Commodore64Host systemHost;

    private final NMOS6510<?> cpu;
    private final MOS6569 vic2;
    private final MOS6581 sid;
    private final MOS6526 cia1;
    private final MOS6526 cia2;

    private final int phiInFrequencyHz;
    private final int framerate;
    private final int iterationsPerFrame;

    public Commodore64Emulator(Commodore64Host systemHost) {
        this.systemHost = systemHost;

        this.phiInFrequencyHz = PAL_PHI_IN_HZ;
        this.framerate = PAL_FRAMERATE;
        this.iterationsPerFrame = this.phiInFrequencyHz / CPU_CLOCK_DIVISOR;

        this.cpu = new NMOS6510<>(this);
        this.vic2 = new MOS6569();
        this.sid = new MOS6581();
        this.cia1 = new MOS6526();
        this.cia2 = new MOS6526();
    }

    @Override
    public Commodore64Host getHost() {
        return this.systemHost;
    }

    @Override
    public Bus getBus() {
        return null;
    }

    @Override
    public VideoGenerator getVideoGenerator() {
        return this.vic2;
    }

    @Override
    public AudioGenerator getAudioGenerator() {
        return this.sid;
    }

    @Override
    public SystemController getSystemController() {
        return null;
    }

    public MOS6526 getCIA1() {
        return this.cia1;
    }

    public MOS6526 getCIA2() {
        return this.cia2;
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
        switch (this.cpu.getHalfCyclePhase()) {
            case PHI_1 -> {
                this.cpu.cycle();
                this.vic2.clockBusAccess();
            }
            case PHI_2 -> {
                this.cpu.cycle();

                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
            }
        }
    }

    @Override
    public int getFramerate() {
        return this.framerate;
    }

    @Override
    public boolean getAEC() {
        return this.vic2.getAEC();
    }

    @Override
    public int readIO(int ddr) {
        return 0;
    }

    @Override
    public void writeIO(int value, int ddr) {

    }

    @Override
    public boolean getIRQ() {
        return this.vic2.getIRQ();
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
        return this.vic2.getBA();
    }

    @Override
    public void close() throws Exception {

    }

}
