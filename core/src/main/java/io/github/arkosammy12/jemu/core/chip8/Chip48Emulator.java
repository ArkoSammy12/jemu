package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.AbstractChip8AudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.audio.Chip48AudioGenerator;
import org.jetbrains.annotations.NotNull;

import static io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8Interpreter.DRAW_EXECUTED;

public class Chip48Emulator extends Chip8Emulator {

    private Chip48AudioGenerator<?> audioGenerator;

    public Chip48Emulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public AbstractChip8AudioGenerator<?> getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    @NotNull
    protected AbstractChip8AudioGenerator<?> createAudio() {
        this.audioGenerator = new Chip48AudioGenerator<>(this);
        return this.audioGenerator;
    }

    @Override
    public int getFramerate() {
        return 64;
    }

    @Override
    protected boolean waitVBlank(int flags) {
        return this.getHost().doDisplayWait() && (flags & DRAW_EXECUTED) != 0;
    }

}
