package io.github.arkosammy12.jemu.core.chip8.audio;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class AbstractChip8AudioGenerator<E extends Chip8Emulator> implements AudioGenerator {

    protected final E emulator;

    private final SampleFrameResampler sampleFrameResampler;

    public AbstractChip8AudioGenerator(E emulator) {
        this.emulator = emulator;
        this.sampleFrameResampler = this.createSampleFrameResampler();
    }

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    @NotNull
    public SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.sampleFrameResampler;
    }

    @Override
    public Optional<? extends SampleFrame> getSampleFrame() {
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.createSampleFrame());
    }

    abstract protected SampleFrameResampler createSampleFrameResampler();

    abstract protected AudioGenerator.SampleFrame createSampleFrame();

    abstract public void onFrame();

}
