package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.AbstractChip8AudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.audio.Chip8AudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.display.SuperChipModernDisplay;
import io.github.arkosammy12.jemu.core.chip8.interpreters.SuperChipModernInterpreter;
import org.jetbrains.annotations.NotNull;

import static io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8Interpreter.DRAW_EXECUTED;

public class SuperChipModernEmulator extends SuperChip11Emulator {

    private SuperChipModernInterpreter<?> interpreter;
    private SuperChipModernDisplay<?> display;
    private Chip8AudioGenerator<?> audioGenerator;

    public SuperChipModernEmulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public SuperChipModernInterpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public SuperChipModernDisplay<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    public Chip8AudioGenerator<?> getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    @NotNull
    protected SuperChipModernInterpreter<?> createInterpreter() {
        this.interpreter = new SuperChipModernInterpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected SuperChipModernDisplay<?> createDisplay() {
        this.display = new SuperChipModernDisplay<>(this);
        return this.display;
    }

    @Override
    @NotNull
    protected AbstractChip8AudioGenerator<?> createAudio() {
        this.audioGenerator = new Chip8AudioGenerator<>(this);
        return this.audioGenerator;
    }

    @Override
    public int getFramerate() {
        return 60;
    }

    @Override
    protected boolean waitVBlank(int flags) {
        return this.getSettings().doDisplayWait() && (flags & DRAW_EXECUTED) != 0;
    }

}
