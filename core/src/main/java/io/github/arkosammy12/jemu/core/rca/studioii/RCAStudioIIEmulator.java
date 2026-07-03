package io.github.arkosammy12.jemu.core.rca.studioii;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.rca.CDP1802System;
import io.github.arkosammy12.jemu.core.rca.CDP1861;
import io.github.arkosammy12.jemu.core.rca.ToneGenerator;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

public class RCAStudioIIEmulator implements CDP1802System, CDP1802.SystemBus, Resetable {

    private final SystemHost systemHost;
    private String loadedRomFileName = "";

    private final CDP1802 cpu;
    private final RCAStudioIIBus bus;
    private final CDP1861<?> vdp;
    private final AudioGenerator audioGenerator;
    private final RCAStudioIIKeypad keypad;

    private final Runnable runCycleFunction;
    private final Runnable resetRunCycleFunction;
    private Runnable currentRunCycleFunction;

    private final BooleanSupplier deassertCLEARSupplier;
    private final BooleanSupplier assertCLEARSupplier;
    private BooleanSupplier clearLineLevelSupplier;

    public RCAStudioIIEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new CDP1802(this);
        this.vdp = new CDP1861<>(this);
        this.audioGenerator = new ToneGenerator<>(this);
        this.keypad = new RCAStudioIIKeypad();

        this.bus = new RCAStudioIIBus();

        Path romPath = systemHost.getRomPath().orElse(null);
        this.bus.initializeCartridge(romPath, systemHost.getRom().orElse(null));
        if (romPath != null) {
            this.loadedRomFileName = romPath.getFileName().toString();
        }

        this.runCycleFunction = () -> {
            this.cpu.cycle();
            this.vdp.cycle();
            this.cpu.nextState();
        };

        this.resetRunCycleFunction = () -> {
            Path path = systemHost.getRomPath().orElse(null);
            String romPathFileName = path == null ? "" : path.getFileName().toString();
            if (!this.loadedRomFileName.equals(romPathFileName)) {
                this.loadedRomFileName = romPathFileName;
                this.bus.initializeCartridge(path, systemHost.getRom().orElse(null));
            }

            this.vdp.reset();
            this.runCycleFunction.run();
            this.currentRunCycleFunction = this.runCycleFunction;
        };

        this.currentRunCycleFunction = this.runCycleFunction;

        this.deassertCLEARSupplier = () -> false;
        this.assertCLEARSupplier = () -> {
            this.clearLineLevelSupplier = this.deassertCLEARSupplier;
            return true;
        };
        this.clearLineLevelSupplier = this.deassertCLEARSupplier;
    }

    @Override
    public SystemHost getHost() {
        return this.systemHost;
    }

    @Override
    public CDP1802 getCpu() {
        return this.cpu;
    }

    @Override
    public Bus getBus() {
        return this.bus;
    }

    @Override
    public VideoGenerator getVideoGenerator() {
        return this.vdp;
    }

    @Override
    public AudioGenerator getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    public SystemController getSystemController() {
        return this.keypad;
    }

    @Override
    public void executeFrame() {
        for (int i = 0; i < CDP1861.CPU_CYCLES_PER_FRAME; i++) {
            this.runCycle();
        }
    }

    @Override
    public void executeCycle() {
        this.runCycle();
    }

    private void runCycle() {
        this.currentRunCycleFunction.run();
    }

    @Override
    public int getFramerate() {
        return 60;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean getCLEAR() {
        return this.clearLineLevelSupplier.getAsBoolean();
    }

    @Override
    public boolean getWAIT() {
        return false;
    }

    @Override
    public boolean getDMAIN() {
        return false;
    }

    @Override
    public boolean getINT() {
        return this.vdp.getINT();
    }

    @Override
    public boolean getDMAOUT() {
        return this.vdp.getDMAO();
    }

    @Override
    public boolean getEF1() {
        return this.vdp.getEFX();
    }

    @Override
    public boolean getEF2() {
        return false;
    }

    @Override
    public boolean getEF3() {
        return this.keypad.getKeypad1EFX();
    }

    @Override
    public boolean getEF4() {
        return this.keypad.getKeypad2EFX();
    }

    @Override
    public int readDMAIN(int dmaInAddress) {
        return 0xFF;
    }

    @Override
    public void writeDMAOUT(int dmaOutAddress, int value) {
        if (this.vdp.getDMAO()) {
            this.vdp.onDMAOUT(dmaOutAddress, value);
        }
    }

    public int readIO(int ioPort) {
        if ((ioPort & 1) != 0) {
            this.vdp.setDisplayEnable(true);
        }
        return 0xFF;
    }

    public void writeIO(int ioPort, int value) {
        if ((ioPort & 1) != 0) {
            this.vdp.setDisplayEnable(false);
        }
        if ((ioPort & 0b10) != 0) {
            this.keypad.setLatchedKey(value);
        }
    }

    @Override
    public void reset() {
        this.currentRunCycleFunction = this.resetRunCycleFunction;
        this.clearLineLevelSupplier = this.assertCLEARSupplier;
    }

}
