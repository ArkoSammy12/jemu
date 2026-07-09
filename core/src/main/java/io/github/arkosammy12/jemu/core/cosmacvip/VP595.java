package io.github.arkosammy12.jemu.core.cosmacvip;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.hardware.ToneGenerator.SQUARE_WAVE_AMPLITUDE;

public class VP595<E extends CosmacVIPEmulator> implements AudioGenerator {

    private final E emulator;
    private final SampleFrameResampler resampler;

    private double frequencyLatch = 27535.0 / (0x80 + 1);

    public VP595(E emulator) {
        this.emulator = emulator;
        this.resampler = new SampleFrameResampler() {

            private double phase = 0.0;

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(boolean q, double frequency)) {
                    byte[] data = new byte[outputSamplesPerFrame];
                    double step = frequency / (double) outputSampleRate;
                    if (q) {
                        for (int i = 0; i < data.length; i++) {
                            data[i] = (byte) ((phase < 0.5) ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
                            phase = (phase + step) % 1.0;
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

    public void setFrequency(double value) {
        double actualValue = value != 0 ? value : 0x80;
        this.frequencyLatch = 27535.0 / (actualValue + 1.0);
    }

    @Override
    public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SampleFrame(this.emulator.getCpu().getQ(), this.frequencyLatch));
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.resampler;
    }

    private record SampleFrame(boolean q, double frequencyLatch) implements AudioGenerator.SampleFrame {}

}
