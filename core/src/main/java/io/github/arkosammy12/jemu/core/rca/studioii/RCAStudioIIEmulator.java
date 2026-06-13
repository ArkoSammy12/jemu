package io.github.arkosammy12.jemu.core.rca.studioii;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.rca.CDP1802System;
import io.github.arkosammy12.jemu.core.rca.CDP1861;
import io.github.arkosammy12.jemu.core.rca.ToneGenerator;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;
import org.apache.commons.io.FilenameUtils;

import java.util.Optional;

public class RCAStudioIIEmulator implements CDP1802System, CDP1802.SystemBus {

    private final SystemHost systemHost;

    private final CDP1802 cpu;
    private final Bus bus;
    private final CDP1861<?> vdp;
    private final AudioGenerator audioGenerator;
    private final RCAStudioIIKeypad keypad;

    public RCAStudioIIEmulator(SystemHost systemHost) {
        this.systemHost = systemHost;
        this.cpu = new CDP1802(this);
        this.vdp = new CDP1861<>(this);
        this.audioGenerator = new ToneGenerator<>(this);
        this.keypad = new RCAStudioIIKeypad();

        Optional<Boolean> isSt2File = systemHost.getRomPath().map(path -> FilenameUtils.getExtension(path.toString()).equals("st2"));
        Optional<byte[]> rom = systemHost.getRom();
        if (rom.isPresent() && isSt2File.isPresent() && isSt2File.get()) {
            this.bus = new St2LoadedBus(rom.get());
        } else {
            this.bus = new RCAStudioIIBus(this);
        }
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
        this.cpu.cycle();
        this.vdp.cycle();
        this.cpu.nextState();
    }

    @Override
    public int getFramerate() {
        return 60;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean getDMAIN() {
        return false;
    }

    @Override
    public boolean getDMAOUT() {
        return this.vdp.getDMAOUTSignal();
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
    public boolean getINT() {
        return this.vdp.getInterruptSignal();
    }

    @Override
    public int readDMAIN(int dmaInAddress) {
        return 0xFF;
    }

    @Override
    public void writeDMAOUT(int dmaOutAddress, int value) {
        if (this.vdp.getDMAOUTSignal()) {
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

}
