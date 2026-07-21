package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.AbstractChip8AudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.audio.Chip8AudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.chip8.display.Chip8Display;
import io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8Interpreter;
import io.github.arkosammy12.jemu.core.common.*;

import static io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8Interpreter.*;

public class Chip8Emulator implements Emulator {

    private static final int IPF_THROTTLE_THRESHOLD = 1000000;

    private final Chip8Host systemHost;

    private final Chip8Interpreter<?> interpreter;
    private final Chip8Bus bus;
    private final Chip8Display<?> display;
    private final AbstractChip8AudioGenerator<?> audio;
    private final Chip8Keypad keypad;

    private final long frameInterval = 1_000_000_000L / this.getFramerate();

    private int targetInstructionsPerFrame = 1;
    private int currentInstructionsPerFrame = 1;
    private boolean longInstruction;

    public Chip8Emulator(Chip8Host systemHost) {
        this.systemHost = systemHost;

        this.keypad = new Chip8Keypad();
        this.audio = this.createAudio();
        this.display = this.createDisplay();
        this.bus = this.createBus();
        this.interpreter = this.createInterpreter();

        this.bus.loadFont(this.systemHost.getSpriteFont());
    }

    protected Chip8Interpreter<?> createInterpreter() {
        return new Chip8Interpreter<>(this);
    }

    protected Chip8Bus createBus() {
        return new Chip8Bus(this);
    }

    protected Chip8Display<?> createDisplay() {
        return new Chip8Display<>(this);
    }

    protected AbstractChip8AudioGenerator<?> createAudio() {
        return new Chip8AudioGenerator<>(this);
    }

    @Override
    public Chip8Host getHost() {
        return systemHost;
    }

    @Override
    public Chip8Display<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    public AbstractChip8AudioGenerator<?> getAudioGenerator() {
        return this.audio;
    }

    @Override
    public Chip8Keypad getSystemController() {
        return this.keypad;
    }

    public Chip8Interpreter<?> getInterpreter() {
        return this.interpreter;
    }

    public Chip8Bus getBus() {
        return this.bus;
    }

    public Chip8Host.Settings getSettings() {
        return this.systemHost.getSettings();
    }

    @Override
    public int getFramerate() {
        return 60;
    }

    public void setTargetInstructionsPerFrame(int ipf) {
        if (ipf < 1) {
            throw new IllegalArgumentException("The IPF value cannot be less than 1!");
        }
        this.targetInstructionsPerFrame = ipf;
        this.currentInstructionsPerFrame = ipf;
    }

    @Override
    public void executeFrame() {
        long startOfFrame = System.nanoTime();

        this.runInstructionLoop();
        this.display.onFrame();
        this.audio.onFrame();

        long endOfFrame = System.nanoTime();
        long frameTime = endOfFrame - startOfFrame;
        if (this.targetInstructionsPerFrame >= IPF_THROTTLE_THRESHOLD) {
            long adjust = (frameTime - this.frameInterval) / 100;
            this.currentInstructionsPerFrame = Math.clamp(this.currentInstructionsPerFrame - adjust, 1, this.targetInstructionsPerFrame);
        }
    }

    @Override
    public void executeCycle() {
        if (this.interpreter.getInstructionCount() % this.currentInstructionsPerFrame == 0) {
            this.interpreter.decrementTimers();
        }
        this.interpreter.cycle();
        if (this.interpreter.shouldExit()) {
            this.terminate();
        }
        this.display.onFrame();
        this.audio.onFrame();
    }

    private void runInstructionLoop() {
        this.interpreter.decrementTimers();
        if (this.longInstruction) {
            this.longInstruction = false;
            return;
        }

        int ipf = this.currentInstructionsPerFrame;
        for (int i = 0; i < ipf; i++) {
            int flags = this.interpreter.cycle();
            if (this.waitVBlank(flags)) {
                break;
            }
            if (this.interpreter.shouldExit()) {
                this.terminate();
                break;
            }
        }
    }

    protected boolean waitVBlank(int flags) {
        if (this.getSettings().doDisplayWait()) {
            if ((flags & LONG_INSTRUCTION) != 0) {
                this.longInstruction = true;
                return true;
            } else return (flags & DRAW_EXECUTED) != 0;
        }
        return false;
    }

    private void terminate() {
        // TODO
    }

    @Override
    public void close() throws Exception {

    }

}
