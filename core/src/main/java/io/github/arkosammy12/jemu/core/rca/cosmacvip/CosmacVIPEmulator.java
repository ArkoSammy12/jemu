package io.github.arkosammy12.jemu.core.rca.cosmacvip;

import io.github.arkosammy12.jemu.core.common.*;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;
import io.github.arkosammy12.jemu.core.rca.CDP1802System;
import io.github.arkosammy12.jemu.core.rca.CDP1861;
import io.github.arkosammy12.jemu.core.rca.ToneGenerator;

public class CosmacVIPEmulator implements CDP1802System, CDP1802.SystemBus {

    private final CosmacVIPHost host;
    private final CosmacVIPHost.Chip8Interpreter chip8Interpreter;

    private final CDP1802 cpu;
    private final CosmacVIPBus bus;
    private final CDP1861<?> vdp;
    private final AudioGenerator audioGenerator;
    private final CosmacVIPKeypad keypad;

    private final int frameRate;

    public CosmacVIPEmulator(CosmacVIPHost host) {
        try {
            this.host = host;
            this.chip8Interpreter = host.getChip8Interpreter();
            this.keypad = new CosmacVIPKeypad(host.getRom().isEmpty());
            this.cpu = new CDP1802(this);
            this.bus = new CosmacVIPBus(this);
            if (this.chip8Interpreter == CosmacVIPHost.Chip8Interpreter.CHIP_8X) {
                this.vdp = new VP590<>(this);
                this.audioGenerator = new VP595<>(this);
                this.frameRate = 61;
            } else {
                this.vdp = new CDP1861<>(this);
                this.audioGenerator = new ToneGenerator<>(this);
                this.frameRate = 60;
            }
        } catch (Exception e) {
            throw new EmulatorException(e);
        }
    }

    @Override
    public SystemHost getHost() {
        return this.host;
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
    public CDP1861<?> getVideoGenerator() {
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

    public CosmacVIPHost.Chip8Interpreter getChip8Interpreter() {
        return this.chip8Interpreter;
    }

    @Override
    public int getFramerate() {
        return this.frameRate;
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
    public void close() {

    }

    @Override
    public boolean getCLEAR() {
        return false;
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
        return this.keypad.getEFX();
    }

    @Override
    public boolean getEF4() {
        return false;
    }

    @Override
    public boolean getINT() {
        return this.vdp.getINT();
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
        if (ioPort == 1) {
            this.vdp.setDisplayEnable(true);
        }
        return 0xFF;
    }

    public void writeIO(int ioPort, int value) {
        if ((ioPort & 4) != 0) {
            this.bus.unlatchAddressMSB();
        }
        switch (ioPort) {
            case 1 -> this.vdp.setDisplayEnable(false);
            case 2 -> this.keypad.setLatchedKey(value);
            case 3 -> {
                if (this.audioGenerator instanceof VP595<?> vp595) {
                    vp595.setFrequency(value);
                }
            }
            case 5 -> {
                if (this.vdp instanceof VP590<?> vp590) {
                    vp590.incrementBackgroundColorIndex();
                }
            }
        }
    }

}