package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.display.SuperChip10Display;
import io.github.arkosammy12.jemu.core.chip8.interpreters.SuperChip10Interpreter;
import org.jetbrains.annotations.NotNull;

public class SuperChip10Emulator extends Chip48Emulator {

    private SuperChip10Interpreter<?> interpreter;
    private SuperChip10Display<?> display;

    public SuperChip10Emulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public SuperChip10Interpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public SuperChip10Display<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    @NotNull
    protected SuperChip10Interpreter<?> createInterpreter() {
        this.interpreter = new SuperChip10Interpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected SuperChip10Display<?> createDisplay() {
        this.display = new SuperChip10Display<>(this);
        return this.display;
    }

    @Override
    protected boolean waitVBlank(int flags) {
        return super.waitVBlank(flags) && !this.getVideoGenerator().isHiresEnabled();
    }

}
