package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.bus.StrictChip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.StrictChip8Display;
import io.github.arkosammy12.jemu.core.chip8.interpreters.StrictChip8Interpreter;

public final class StrictChip8Emulator extends Chip8Emulator {

    private StrictChip8Interpreter processor;
    private StrictChip8Bus bus;
    private StrictChip8Display display;

    private long machineCycles;
    private long nextFrame;
    private int cycleCounter;

    public StrictChip8Emulator(Chip8Host systemHost) {
        super(systemHost);
        // Amount of cycles the COSMAC-VIP needs to set things up before beginning execution of the ROM
        this.machineCycles = 3250;
        this.nextFrame = this.calculateNextFrame();
    }

    @Override
    public StrictChip8Interpreter getInterpreter() {
        return this.processor;
    }

    @Override
    public StrictChip8Display getVideoGenerator() {
        return this.display;
    }

    @Override
    public StrictChip8Bus getBus() {
        return this.bus;
    }

    @Override
    protected StrictChip8Interpreter createInterpreter() {
        this.processor = new StrictChip8Interpreter(this);
        return this.processor;
    }

    @Override
    protected StrictChip8Display createDisplay() {
        this.display = new StrictChip8Display(this);
        return this.display;
    }

    @Override
    protected StrictChip8Bus createBus() {
        this.bus = new StrictChip8Bus(this);
        return this.bus;
    }

    @Override
    public void executeFrame() {
        long nextFrame = this.nextFrame;
        while (this.machineCycles < nextFrame) {
            this.getInterpreter().cycle();
        }
    }

    @Override
    public void executeCycle() {
        this.getInterpreter().cycle();
    }

    public void addCycles(long cycles) {
        this.machineCycles += cycles;
        if (this.machineCycles >= this.nextFrame) {
            long irqTime = 1832 + (this.getInterpreter().getST() != 0 ? 4 : 0)  + (this.getInterpreter().getDT() != 0 ? 8 : 0);
            this.handleInterrupt();
            this.machineCycles += irqTime;
            this.nextFrame = this.calculateNextFrame();
        }
    }

    public long getCyclesLeftInCurrentFrame() {
        return this.nextFrame - this.machineCycles;
    }

    private void handleInterrupt() {
        this.getAudioGenerator().onFrame();
        this.getInterpreter().decrementTimers();
        this.getVideoGenerator().onFrame();
    }

    private long calculateNextFrame() {
        return ((this.machineCycles + 2572) / 3668) * 3668 + 1096;
    }

}
