package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.hardware.NMOS6502;
import io.github.arkosammy12.jemu.core.hardware.NMOS6510;
import io.github.arkosammy12.jemu.core.util.MOSIOPort;

public class Commodore64Emulator implements Emulator, NMOS6510.SystemBus {

    private static final int CPU_CLOCK_DIVISOR = 8;

    private static final int PAL_PHI_IN_HZ = 7_881_990;
    private static final int PAL_FRAMERATE = 50;

    private final Commodore64Host systemHost;

    private final NMOS6510<?> cpu;
    private final Commodore64Bus<?> bus;
    private final MOS6569<?> vic2;
    private final MOS6581 sid;
    private final MOS6526 cia1;
    private final MOS6526 cia2;
    private final Commodore64Controller systemController;

    private final MOSIOPort cpuIOPort;
    private final MOSIOPort cia1IOPort;
    private final MOSIOPort cia2IOPort;

    private final int phiInFrequencyHz;
    private final int framerate;
    private final int iterationsPerFrame;

    public Commodore64Emulator(Commodore64Host systemHost) {
        this.systemHost = systemHost;

        this.phiInFrequencyHz = PAL_PHI_IN_HZ;
        this.framerate = PAL_FRAMERATE;
        this.iterationsPerFrame = this.phiInFrequencyHz / CPU_CLOCK_DIVISOR;

        this.cpuIOPort = new MOSIOPort(index -> switch (index & 0b111) {
            case 0, 1, 2 -> true;
            default -> false;
        });

        this.cia1IOPort = new MOSIOPort(_ -> false);
        this.cia2IOPort = new MOSIOPort(_ -> false);

        this.bus = new Commodore64Bus<>(this);
        this.cpu = new NMOS6510<>(this);
        this.vic2 = new MOS6569<>(this);
        this.sid = new MOS6581();
        this.cia1 = new MOS6526();
        this.cia2 = new MOS6526();
        this.systemController = new Commodore64Controller();
    }

    @Override
    public Commodore64Host getHost() {
        return this.systemHost;
    }

    @Override
    public Commodore64Bus<?> getBus() {
        return this.bus;
    }

    @Override
    public MOS6569<?> getVideoGenerator() {
        return this.vic2;
    }

    @Override
    public MOS6581 getAudioGenerator() {
        return this.sid;
    }

    @Override
    public SystemController getSystemController() {
        return this.systemController;
    }

    public MOS6526 getCIA1() {
        return this.cia1;
    }

    public MOS6526 getCIA2() {
        return this.cia2;
    }

    public MOSIOPort getCPUIOPort() {
        return this.cpuIOPort;
    }

    public MOSIOPort getCIA1IOPort() {
        return this.cia1IOPort;
    }

    public MOSIOPort getCIA2IOPort() {
        return this.cia2IOPort;
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
                this.vic2.clockBusAccess(NMOS6502.Phase.PHI_1);
            }
            case PHI_2 -> {
                this.cpu.cycle();

                this.vic2.clockBusAccess(NMOS6502.Phase.PHI_2);
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();
                this.vic2.clockPixel();

                this.cia1.cycle();
                this.cia2.cycle();

                this.sid.cycle();
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
    public MOSIOPort getIOPort() {
        return this.cpuIOPort;
    }

    @Override
    public boolean getIRQ() {
        return this.vic2.getIRQ();
    }

    @Override
    public boolean getNMI() {
        // TODO: Wired to RESTORE key and CIA2
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
