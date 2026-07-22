package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.chip8.audio.MegaChipAudioGenerator;
import io.github.arkosammy12.jemu.core.chip8.bus.MegaChipBus;
import io.github.arkosammy12.jemu.core.chip8.display.MegaChipDisplay;
import io.github.arkosammy12.jemu.core.chip8.interpreters.MegaChipInterpreter;
import org.jetbrains.annotations.NotNull;

import static io.github.arkosammy12.jemu.core.chip8.interpreters.Chip8Interpreter.CLS_EXECUTED;

public class MegaChipEmulator extends SuperChip11Emulator {

    private MegaChipInterpreter<?> interpreter;
    private MegaChipBus bus;
    private MegaChipDisplay<?> display;
    private MegaChipAudioGenerator<?> audioGenerator;

    public MegaChipEmulator(Chip8Host systemHost) {
        super(systemHost);
    }

    @Override
    public MegaChipInterpreter<?> getInterpreter() {
        return this.interpreter;
    }

    @Override
    public MegaChipBus getBus() {
        return this.bus;
    }

    @Override
    public MegaChipDisplay<?> getVideoGenerator() {
        return this.display;
    }

    @Override
    public MegaChipAudioGenerator<?> getAudioGenerator() {
        return this.audioGenerator;
    }

    @Override
    @NotNull
    protected MegaChipInterpreter<?> createInterpreter() {
        this.interpreter = new MegaChipInterpreter<>(this);
        return this.interpreter;
    }

    @Override
    @NotNull
    protected MegaChipBus createBus() {
        this.bus = new MegaChipBus(this);
        return this.bus;
    }

    @Override
    @NotNull
    protected MegaChipDisplay<?> createDisplay() {
        this.display = new MegaChipDisplay<>(this);
        return this.display;
    }

    @Override
    @NotNull
    protected MegaChipAudioGenerator<?> createAudio() {
        this.audioGenerator = new MegaChipAudioGenerator<>(this);
        return this.audioGenerator;
    }

    @Override
    public int getFramerate() {
        return 50;
    }

    @Override
    protected boolean waitVBlank(int flags) {
        return this.getInterpreter().isMegaModeEnabled() ? (flags & CLS_EXECUTED) != 0 : super.waitVBlank(flags);
    }

}
