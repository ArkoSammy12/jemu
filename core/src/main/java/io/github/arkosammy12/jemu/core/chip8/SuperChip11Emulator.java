package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.display.SuperChip11Display;
import io.github.arkosammy12.jemu.core.chip8.interpreters.SuperChip11Interpreter;
import org.jetbrains.annotations.NotNull;

public class SuperChip11Emulator extends SuperChip10Emulator {

    private SuperChip11Interpreter<?> interpreter;
    private SuperChip11Display<?> display;

    public SuperChip11Emulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public SuperChip11Interpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public SuperChip11Display<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    @NotNull
    protected SuperChip11Interpreter<?> createInterpreter() {
        this.interpreter = new SuperChip11Interpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected SuperChip11Display<?> createDisplay() {
        this.display = new SuperChip11Display<>(this);
        return this.display;
    }


}
