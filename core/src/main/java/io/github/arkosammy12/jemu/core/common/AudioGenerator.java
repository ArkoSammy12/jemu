package io.github.arkosammy12.jemu.core.common;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface AudioGenerator {

    boolean isStereo();

    @NotNull
    SampleSize getBytesPerSample();

    Optional<SampleFrame> getSampleFrame();

    SampleFrameResampler getSampleFrameResampler();

    enum SampleSize {
        BYTES_1,
        BYTES_2
    }

    interface SampleFrame {

    }

    interface SampleFrameResampler {

        Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, SampleFrame inputSampleFrame);

    }

}
