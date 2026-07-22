package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.Chip8XAudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8XBus;
import io.github.arkosammy12.jemu.core.chip8.display.Chip8XDisplay;
import io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8XInterpreter;
import org.jetbrains.annotations.NotNull;

public class Chip8XEmulator extends Chip8Emulator {

    private Chip8XInterpreter<?> interpreter;
    private Chip8XBus bus;
    private Chip8XDisplay<?> display;
    private Chip8XAudioGenerator<?> audioGenerator;

    public Chip8XEmulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public Chip8XInterpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public Chip8XBus getBus() {
        return this.bus;
    }

    @Override
    public Chip8XDisplay<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    public Chip8XAudioGenerator<?> getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    @NotNull
    protected Chip8XInterpreter<?> createInterpreter() {
        this.interpreter = new Chip8XInterpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected Chip8XBus createBus() {
        this.bus = new Chip8XBus(this);
        return this.bus;
    }

    @Override
    @NotNull
    protected Chip8XDisplay<?> createDisplay() {
        this.display = new Chip8XDisplay<>(this);
        return this.display;
    }

    @Override
    @NotNull
    protected Chip8XAudioGenerator<?> createAudio() {
        this.audioGenerator = new Chip8XAudioGenerator<>(this);
        return this.audioGenerator;
    }

    @Override
    public int getFramerate() {
        return 61;
    }

}
