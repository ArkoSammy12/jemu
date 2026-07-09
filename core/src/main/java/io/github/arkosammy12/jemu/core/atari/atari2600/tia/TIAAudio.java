package io.github.arkosammy12.jemu.core.atari.atari2600.tia;

import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.atari.atari2600.tia.TIA.*;

class TIAAudio<E extends Emulator & TIA.SystemBus> implements Bus, AudioGenerator {

    private static final int AUDIO_CPU_CLK_DIVISOR = 38;
    private static final double OUTPUT_GAIN = Short.MAX_VALUE;

    private final E emulator;
    private final SampleFrameResampler sampleFrameResampler;

    private final AudioChannel channel0 = new AudioChannel();
    private final AudioChannel channel1 = new AudioChannel();

    private final double[] sampleBuffer;
    private int currentSampleIndex;

    private int audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;

    TIAAudio(E emulator, int samplesPerFrame) {
        this.emulator = emulator;
        this.sampleBuffer = new double[samplesPerFrame];
        this.sampleFrameResampler = (_, outputSamplesPerFrame, inputSampleFrame) -> {
            if (inputSampleFrame instanceof SampleFrame(double[] sampleFrame)) {

                byte[] out = new byte[outputSamplesPerFrame * 2];
                double step = (double) sampleFrame.length / (double) outputSamplesPerFrame;
                double pos = 0.0;

                for (int i = 0; i < outputSamplesPerFrame; i++) {
                    int index = Math.min((int) Math.round(pos), sampleFrame.length - 1);
                    short sample = (short) Math.clamp((long)(sampleFrame[index] * OUTPUT_GAIN), -Short.MAX_VALUE, Short.MAX_VALUE);
                    out[i * 2] = (byte) ((int) sample & 0xFF);
                    out[i * 2 + 1] = (byte) (((int) sample >>> 8) & 0xFF);
                    pos += step;
                }

                return Optional.of(out);
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
        return SampleSize.BYTES_2;
    }

    @Override
    public Optional<AudioGenerator.SampleFrame> getSampleFrame() {
        this.currentSampleIndex = 0;
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SampleFrame(Arrays.copyOf(this.sampleBuffer, this.sampleBuffer.length)));
    }

    @Override
    public SampleFrameResampler getSampleFrameResampler() {
        return this.sampleFrameResampler;
    }

    void cycle() {
        this.audioClockDivisor--;
        if (this.audioClockDivisor <= 0) {
            this.audioClockDivisor = AUDIO_CPU_CLK_DIVISOR;
            this.channel0.clock();
            this.channel1.clock();
        }
        double ch0 = this.channel0.getDigitalOutput();
        double ch1 = this.channel1.getDigitalOutput();
        this.sampleBuffer[this.currentSampleIndex] = (ch0 + ch1) / 31.0;
        this.currentSampleIndex++;
    }

    @Override
    public int readByte(int address) {
        return this.emulator.combineWithDataBus(0, 0x00);
    }

    @Override
    public void writeByte(int address, int value) {
        switch (address) {
            case AUDC0 -> this.channel0.setControl(value);
            case AUDC1 -> this.channel1.setControl(value);
            case AUDF0 -> this.channel0.setFrequency(value);
            case AUDF1 -> this.channel1.setFrequency(value);
            case AUDV0 -> this.channel0.setVolume(value);
            case AUDV1 -> this.channel1.setVolume(value);
        }
    }

    private record SampleFrame(double[] sampleFrame) implements AudioGenerator.SampleFrame {}

    private static class AudioChannel {

        private int control;
        private int frequency;
        private int volume;

        private boolean holdPulseCounter;

        private int frequencyDivisorCounter = 1;
        private int pulseCounter;
        private int noiseCounter;

        private void setControl(int value) {
            this.control = value & 0xF;
        }

        private void setFrequency(int value) {
            this.frequency = (value & 0x1F) + 1;
        }

        private void setVolume(int value) {
            this.volume = value & 0xF;
        }

        // The following implementation of the feedback logic for both LFSRs has been derived from the Atari 2600 emulator Stella,
        // using the source file https://github.com/stella-emu/stella/blob/master/src/emucore/tia/AudioChannel.cxx,
        // itself based on Christian Speckner's 6502.ts (https://github.com/6502ts/6502.ts/blob/master/src/machine/stella/tia/PCMChannel.ts).
        // Many thanks to Stephen Anthony and Christian Speckner for letting me borrow their implementation.
        private void clock() {
            this.frequencyDivisorCounter--;
            if (this.frequencyDivisorCounter <= 0) {
                this.frequencyDivisorCounter = this.frequency;

                // Corresponds to bit 0 of the 5-bit poly counter
                boolean nineBitPolyBit4 = (this.noiseCounter & 1) != 0;

                switch (this.control & 0b11) {
                    case 0 -> {}
                    case 1 -> this.holdPulseCounter = false;
                    case 2 -> this.holdPulseCounter = (this.noiseCounter & 0x1E) != 2;
                    case 3 -> this.holdPulseCounter = !nineBitPolyBit4;
                }

                boolean noiseFeedback;
                if ((this.control & 0b11) == 0) {
                    noiseFeedback = ((this.pulseCounter ^ this.noiseCounter) & 1) != 0 || !(this.noiseCounter != 0 || (this.pulseCounter != 0xA)) || (this.control & 0xC) == 0;
                } else {
                    noiseFeedback = ((((this.noiseCounter & (1 << 2)) != 0) ? 1 : 0) ^ (this.noiseCounter & 1)) != 0 || this.noiseCounter == 0;
                }

                boolean pulseFeedback = switch ((this.control >>> 2) & 0b11) {
                    case 0 -> ((((this.pulseCounter & (1 << 1)) != 0) ? 1 : 0) ^ (this.pulseCounter & 1)) != 0 && (this.pulseCounter != 0xA) && ((this.control & 0b11) != 0);
                    case 1 -> (this.pulseCounter & (1 << 3)) == 0;
                    case 2 -> !nineBitPolyBit4;
                    case 3 -> !(((this.pulseCounter & (1 << 1)) != 0) || (this.pulseCounter & 0xE) == 0);
                    default -> false;
                };

                this.noiseCounter >>>= 1;
                if (noiseFeedback) {
                    this.noiseCounter |= (1 << 4);
                }

                if (!this.holdPulseCounter) {
                    this.pulseCounter = ~(this.pulseCounter >>> 1) & 0b111;

                    if (pulseFeedback) {
                        this.pulseCounter |= (1 << 3);
                    }
                }
            }
        }

        private int getDigitalOutput() {
            return (this.pulseCounter & 1) * this.volume;
        }

    }

}
