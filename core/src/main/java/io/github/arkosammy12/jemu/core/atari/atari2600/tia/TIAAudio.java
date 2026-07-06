package io.github.arkosammy12.jemu.core.atari.atari2600.tia;

import io.github.arkosammy12.jemu.core.atari.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static io.github.arkosammy12.jemu.core.atari.atari2600.tia.TIA.*;

class TIAAudio<E extends Atari2600Emulator> implements Bus, AudioGenerator {

    private final E emulator;
    private final SampleFrameResampler sampleFrameResampler;

    private int currentSampleIndex;

    TIAAudio(E emulator) {
        this.emulator = emulator;
        this.sampleFrameResampler = (outputSampleRate, outputSamplesPerFrame, inputSampleFrame) -> {
            if (inputSampleFrame instanceof SampleFrame) {
                return Optional.of(new byte[outputSamplesPerFrame]);
            } else {
                return Optional.empty();
            }
        };
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
    public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
        this.currentSampleIndex = 0;
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SampleFrame());
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.sampleFrameResampler;
    }

    void clock() {

    }

    @Override
    public int readByte(int address) {
        return this.emulator.getBus().combineWithDataBus(0, 0x00);
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case AUDC0 -> {}
            case AUDC1 -> {}
            case AUDF0 -> {}
            case AUDF1 -> {}
            case AUDV0 -> {}
            case AUDV1 -> {}
        }
    }

    private record SampleFrame() implements AudioGenerator.SampleFrame {}

}
