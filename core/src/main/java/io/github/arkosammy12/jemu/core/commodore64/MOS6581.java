package io.github.arkosammy12.jemu.core.commodore64;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MOS6581 implements AudioGenerator, Bus {

    private static final int VOICE_1_FREQ_LO = 0x00;
    private static final int VOICE_1_FREQ_HI = 0x01;
    private static final int VOICE_1_DUTY_LO = 0x02;
    private static final int VOICE_1_DUTY_HI = 0x03;
    private static final int VOICE_1_WAVEFORM = 0x04;
    private static final int VOICE_1_STOP = 0x05;
    private static final int VOICE_1_KEEP = 0x06;
    private static final int VOICE_2_FREQ_LO = 0x07;
    private static final int VOICE_2_FREQ_HI = 0x08;
    private static final int VOICE_2_DUTY_LO = 0x09;
    private static final int VOICE_2_DUTY_HI = 0x0A;
    private static final int VOICE_2_WAVEFORM = 0x0B;
    private static final int VOICE_2_STOP = 0x0C;
    private static final int VOICE_2_KEEP = 0x0D;
    private static final int VOICE_3_FREQ_LO = 0x0E;
    private static final int VOICE_3_FREQ_HI = 0x0F;
    private static final int VOICE_3_DUTY_LO = 0x10;
    private static final int VOICE_3_DUTY_HI = 0x11;
    private static final int VOICE_3_WAVEFORM = 0x12;
    private static final int VOICE_3_STOP = 0x13;
    private static final int VOICE_3_KEEP = 0x14;
    private static final int FILTER_CUTOFF_FREQ_LO = 0x15;
    private static final int FILTER_CUTOFF_FREQ_HI = 0x16;
    private static final int FILTER_RESONANCE_ROUTING = 0x17;
    private static final int VOICES_VOLUME_BAND_PASS = 0x18;
    private static final int PADDLE_X = 0x19;
    private static final int PADDLE_Y = 0x1A;
    private static final int VOICE_3_OSCILLATOR = 0x1B;
    private static final int VOICE_3_ENVELOPE = 0x1C;

    @Override
    public boolean isStereo() {
        return false;
    }

    @Override
    public @NotNull SampleSize getBytesPerSample() {
        return SampleSize.BYTES_1;
    }

    @Override
    public Optional<? extends SampleFrame> getSampleFrame() {
        return Optional.of(new SampleFrame() {

        });
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return (outputSampleRate, outputSamplesPerFrame, inputSampleFrame) -> Optional.empty();
    }

    @Override
    public int readByte(int address) {
        // TODO: Unused bits and addresses return decaying open bus mutated by SID register accesses, between 4K and 5K cycles
        address &= 0x1F;
        return switch (address) {
            case VOICE_1_FREQ_LO -> 0;
            case VOICE_1_FREQ_HI -> 0;
            case VOICE_1_DUTY_LO -> 0;
            case VOICE_1_DUTY_HI -> 0;
            case VOICE_1_WAVEFORM -> 0;
            case VOICE_1_STOP -> 0;
            case VOICE_1_KEEP -> 0;
            case VOICE_2_FREQ_LO -> 0;
            case VOICE_2_FREQ_HI -> 0;
            case VOICE_2_DUTY_LO -> 0;
            case VOICE_2_DUTY_HI -> 0;
            case VOICE_2_WAVEFORM -> 0;
            case VOICE_2_STOP -> 0;
            case VOICE_2_KEEP -> 0;
            case VOICE_3_FREQ_LO -> 0;
            case VOICE_3_FREQ_HI -> 0;
            case VOICE_3_DUTY_LO -> 0;
            case VOICE_3_DUTY_HI -> 0;
            case VOICE_3_WAVEFORM -> 0;
            case VOICE_3_STOP -> 0;
            case VOICE_3_KEEP -> 0;
            case FILTER_CUTOFF_FREQ_LO -> 0;
            case FILTER_CUTOFF_FREQ_HI -> 0;
            case FILTER_RESONANCE_ROUTING -> 0;
            case VOICES_VOLUME_BAND_PASS -> 0;
            case PADDLE_X -> 0;
            case PADDLE_Y -> 0;
            case VOICE_3_OSCILLATOR -> 0;
            case VOICE_3_ENVELOPE -> 0;
            default -> 0;
        };
    }

    @Override
    public void writeByte(int address, int value) {
        address &= 0x1F;
        switch (address) {
            case VOICE_1_FREQ_LO -> {}
            case VOICE_1_FREQ_HI -> {}
            case VOICE_1_DUTY_LO -> {}
            case VOICE_1_DUTY_HI -> {}
            case VOICE_1_WAVEFORM -> {}
            case VOICE_1_STOP -> {}
            case VOICE_1_KEEP -> {}
            case VOICE_2_FREQ_LO -> {}
            case VOICE_2_FREQ_HI -> {}
            case VOICE_2_DUTY_LO -> {}
            case VOICE_2_DUTY_HI -> {}
            case VOICE_2_WAVEFORM -> {}
            case VOICE_2_STOP -> {}
            case VOICE_2_KEEP -> {}
            case VOICE_3_FREQ_LO -> {}
            case VOICE_3_FREQ_HI -> {}
            case VOICE_3_DUTY_LO -> {}
            case VOICE_3_DUTY_HI -> {}
            case VOICE_3_WAVEFORM -> {}
            case VOICE_3_STOP -> {}
            case VOICE_3_KEEP -> {}
            case FILTER_CUTOFF_FREQ_LO -> {}
            case FILTER_CUTOFF_FREQ_HI -> {}
            case FILTER_RESONANCE_ROUTING -> {}
            case VOICES_VOLUME_BAND_PASS -> {}
            case PADDLE_X -> {}
            case PADDLE_Y -> {}
            case VOICE_3_OSCILLATOR -> {}
            case VOICE_3_ENVELOPE -> {}
        }
    }

    public void cycle() {

    }

}
