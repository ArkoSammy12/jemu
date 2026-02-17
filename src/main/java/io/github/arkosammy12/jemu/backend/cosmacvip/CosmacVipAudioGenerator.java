package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CosmacVipAudioGenerator<E extends CosmacVipEmulator> extends AudioGenerator<E> {

    public static final int SQUARE_WAVE_AMPLITUDE = 4;

    protected double phase = 0.0;

    private static final int[] DEFAULT_PATTERN_1 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] DEFAULT_PATTERN_2 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0
    };

    public CosmacVipAudioGenerator(E emulator) {
        super(emulator);
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
    public Optional<byte[]> getSampleFrame() {
        Optional<AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (!this.emulator.getCpu().getQ() || optionalAudioDriver.isEmpty()) {
            this.phase = 0;
            return Optional.empty();
        }
        AudioDriver audioDriver = optionalAudioDriver.get();
        double step = (4000 * Math.pow(2.0, (175 - 64) / 48.0)) / 128.0 / audioDriver.getSampleRate();
        byte[] data = new byte[audioDriver.getSamplesPerFrame()];
        for (int i = 0; i < data.length; i++) {
            int bitStep = (int) (this.phase * 128);
            data[i] = (byte) (((DEFAULT_PATTERN_2[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
            this.phase = (this.phase + step) % 1.0;
        }
        return Optional.of(data);
    }

}