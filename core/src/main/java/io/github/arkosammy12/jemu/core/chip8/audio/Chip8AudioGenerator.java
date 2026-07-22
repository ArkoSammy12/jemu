package io.github.arkosammy12.jemu.core.chip8.audio;

import io.github.arkosammy12.jemu.core.chip8.Chip8Emulator;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;

import java.util.Optional;

public class Chip8AudioGenerator<E extends Chip8Emulator> extends AbstractChip8AudioGenerator<E> {

    protected boolean playBuzz;

    public Chip8AudioGenerator(E emulator) {
        super(emulator);
    }

    @Override
    protected SampleFrameResampler createSampleFrameResampler() {
        return new SampleFrameResampler() {

            private static final int[] DEFAULT_PATTERN_1 = {
                    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0
            };

            private static final int[] DEFAULT_PATTERN_2 = {
                    0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0
            };

            private static final int SQUARE_WAVE_AMPLITUDE = 4;

            private double phase = 0.0;

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(boolean buzz)) {
                    byte[] out = new byte[outputSamplesPerFrame];
                    if (buzz) {
                        double step = (4000 * Math.pow(2.0, (175 - 64) / 48.0)) / 128.0 / outputSampleRate;
                        for (int i = 0; i < out.length; i++) {
                            int bitStep = (int) (this.phase * 128);
                            out[i] = (byte) (((DEFAULT_PATTERN_2[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
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
        return new SampleFrame(this.playBuzz);
    }

    public void onFrame() {
        this.playBuzz = this.emulator.getInterpreter().getST() > 0;
    }

    protected record SampleFrame(boolean playBuzz) implements AudioGenerator.SampleFrame {}

}
