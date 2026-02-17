package io.github.arkosammy12.jemu.backend.common;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class AudioGenerator<E extends Emulator> {

    protected final E emulator;

    public AudioGenerator(E emulator) {
        this.emulator = emulator;
    }

    abstract public boolean isStereo();

    @NotNull
    abstract public SampleSize getBytesPerSample();

    abstract public Optional<byte[]> getSampleFrame();

    public enum SampleSize {
        BYTES_1,
        BYTES_2
    }

}
