package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.display.HyperWaveChip64Display;
import io.github.arkosammy12.jemu.core.chip8.interpreters.HyperWaveChip64Interpreter;
import org.jetbrains.annotations.NotNull;

public class HyperWaveChip64Emulator extends XOChipEmulator {

    private HyperWaveChip64Interpreter<?> interpreter;
    private HyperWaveChip64Display<?> display;

    public HyperWaveChip64Emulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public HyperWaveChip64Interpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public HyperWaveChip64Display<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    @NotNull
    protected HyperWaveChip64Interpreter<?> createInterpreter() {
        this.interpreter = new HyperWaveChip64Interpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected HyperWaveChip64Display<?> createDisplay() {
        this.display = new HyperWaveChip64Display<>(this);
        return this.display;
    }

}
