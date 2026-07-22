package io.github.arkosammy12.jemu.core.chip8.audio;

import io.github.arkosammy12.jemu.core.chip8.XOChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;

import java.util.Arrays;
import java.util.Optional;

public class XOChipAudioGenerator<E extends XOChipEmulator> extends Chip8AudioGenerator<E> {

    private final int[] patternBuffer = new int[16];
    private int pitch;

    public XOChipAudioGenerator(E emulator) {
        super(emulator);
        this.setPitch(64);
    }

    public void setPitch(int pitch) {
        this.pitch = pitch & 0xFF;
    }

    public void loadAudio(int indexRegister) {
        Chip8Bus bus = this.emulator.getBus();
        for (int i = 0; i < 16; i++) {
            this.patternBuffer[i] = bus.readByte(indexRegister + i);
        }
    }

    @Override
    protected SampleFrameResampler createSampleFrameResampler() {
        return new SampleFrameResampler() {

            private static final int SQUARE_WAVE_AMPLITUDE = 4;

            private double phase = 0.0;

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(boolean buzz, int framePitch, int[] framePatternBuffer)) {
                    byte[] out = new byte[outputSamplesPerFrame];
                    if (buzz) {
                        double step = (4000 * Math.pow(2.0, (framePitch - 64) / 48.0)) / 128.0 / outputSampleRate;
                        for (int i = 0; i < out.length; i++) {
                            int bitStep = (int) (this.phase * 128);
                            out[i] = (byte) (((framePatternBuffer[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
                            this.phase = (this.phase + step) % 1.0;
                        }
                    } else {
                        this.phase = 0;
                    }
                    return Optional.of(out);
                } else {
                    return Optional.empty();
                }
            }

        };
    }

    protected AudioGenerator.SampleFrame createSampleFrame() {
        return new SampleFrame(this.playBuzz, this.pitch, Arrays.copyOf(this.patternBuffer, this.patternBuffer.length));
    }

    private record SampleFrame(boolean playBuzz, int pitch, int[] patternBuffer) implements AudioGenerator.SampleFrame {}

}
