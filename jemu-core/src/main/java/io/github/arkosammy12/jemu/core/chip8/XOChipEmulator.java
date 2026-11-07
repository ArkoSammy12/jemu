package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.XOChipAudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.bus.XOChipBus;
import io.github.arkosammy12.jemu.core.chip8.display.XOChipDisplay;
import io.github.arkosammy12.jemu.core.chip8.interpreters.SuperChipModernInterpreter;
import io.github.arkosammy12.jemu.core.chip8.interpreters.XOChipInterpreter;
import org.jetbrains.annotations.NotNull;

public class XOChipEmulator extends SuperChipModernEmulator {

    private XOChipInterpreter<?> interpreter;
    private XOChipBus bus;
    private XOChipDisplay<?> display;
    private XOChipAudioGenerator<?> audioGenerator;

    public XOChipEmulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public SuperChipModernInterpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public XOChipBus getBus() {
        return this.bus;
    }

    @Override
    public XOChipDisplay<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    public XOChipAudioGenerator<?> getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    @NotNull
    protected XOChipInterpreter<?> createInterpreter() {
        this.interpreter = new XOChipInterpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected XOChipBus createBus() {
        this.bus = new XOChipBus(this);
        return this.bus;
    }

    @Override
    @NotNull
    protected XOChipDisplay<?> createDisplay() {
        this.display = new XOChipDisplay<>(this);
        return this.display;
    }

    @Override
    @NotNull
    protected XOChipAudioGenerator<?> createAudio() {
        this.audioGenerator = new XOChipAudioGenerator<>(this);
        return this.audioGenerator;
    }

}
