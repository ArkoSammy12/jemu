package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NESAPU<E extends NESEmulator> extends AudioGenerator<E> {

    public NESAPU(E emulator) {
        super(emulator);
    }

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    public @NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public Optional<byte[]> getSampleFrame() {
        return Optional.empty();
    }

}
