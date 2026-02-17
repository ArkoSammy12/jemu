package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.backend.cosmacvip.CosmacVipAudioGenerator.SQUARE_WAVE_AMPLITUDE;

public class VP595<E extends CosmacVipEmulator> extends AudioGenerator<E> implements IODevice {

    private double frequencyLatch = 27535.0 / (0x80 + 1);
    private double phase = 0.0;

    public VP595(E emulator) {
        super(emulator);
    }

    @Override
    public boolean isOutputPort(int port) {
        return port == 3;
    }

    @Override
    public void onOutput(int port, int value) {
        int actualValue = value != 0 ? value : 0x80;
        this.frequencyLatch = 27535.0 / (actualValue + 1);
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
        double frequency = frequencyLatch;
        Optional<AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (!this.emulator.getCpu().getQ() || optionalAudioDriver.isEmpty()) {
            phase = 0;
            return Optional.empty();
        }
        AudioDriver audioDriver = optionalAudioDriver.get();
        byte[] data = new byte[audioDriver.getSamplesPerFrame()];
        double step = frequency / audioDriver.getSampleRate();
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((phase < 0.5) ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
            phase = (phase + step) % 1;
        }
        return Optional.of(data);
    }


}
