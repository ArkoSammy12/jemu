package io.github.arkosammy12.jemu.core.rca;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ToneGenerator<E extends CDP1802System> implements AudioGenerator {

    public static final int SQUARE_WAVE_AMPLITUDE = 4;

    private final E emulator;
    private final SampleFrameResampler resampler;

    private static final int[] DEFAULT_PATTERN_1 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] DEFAULT_PATTERN_2 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0
    };

    public ToneGenerator(E emulator) {
        this.emulator = emulator;
        this.resampler = new SampleFrameResampler() {

            private double phase = 0.0;

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(boolean q)) {
                    double step = (4000 * Math.pow(2.0, (175 - 64) / 48.0)) / 128.0 / (double) outputSampleRate;
                    byte[] data = new byte[outputSamplesPerFrame];
                    if (q) {
                        for (int i = 0; i < data.length; i++) {
                            int bitStep = (int) (this.phase * 128);
                            data[i] = (byte) (((DEFAULT_PATTERN_2[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
                            this.phase = (this.phase + step) % 1.0;
                        }
                    } else {
                        this.phase = 0.0;
                    }
                    return Optional.of(data);
                } else {
                    this.phase = 0.0;
                    return Optional.empty();
                }
            }

        };
    }

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    public AudioGenerator.@NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SampleFrame(this.emulator.getCpu().getQ()));
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.resampler;
    }

    private record SampleFrame(boolean q) implements AudioGenerator.SampleFrame {}

}