package io.github.arkosammy12.jemu.core.chip8.audio;

import io.github.arkosammy12.jemu.core.chip8.Chip8XEmulator;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;

import java.util.Optional;

public class Chip8XAudioGenerator<E extends Chip8XEmulator> extends AbstractChip8AudioGenerator<E> {

    private boolean playBuzz;
    private int pitch;

    public Chip8XAudioGenerator(E emulator) {
        super(emulator);
    }

    public void setPitch(int value) {
        this.pitch = value != 0 ? value : 0x80;
    }

    @Override
    protected SampleFrameResampler createSampleFrameResampler() {
        return new SampleFrameResampler() {

            private static final int SQUARE_WAVE_AMPLITUDE = 4;

            private double phase = 0.0;

            @Override
            public Optional<byte[]> resample(int outputSampleRate, int outputSamplesPerFrame, AudioGenerator.SampleFrame inputSampleFrame) {
                if (inputSampleFrame instanceof SampleFrame(boolean buzz, int framePitch)) {
                    byte[] out = new byte[outputSamplesPerFrame];
                    if (buzz) {
                        double step = (27535.0 / (framePitch + 1)) / outputSampleRate;
                        for (int i = 0; i < out.length; i++) {
                            out[i] = (byte) ((phase < 0.5) ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
                            this.phase = (phase + step) % 1;
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

    @Override
    protected AudioGenerator.SampleFrame createSampleFrame() {
        return new SampleFrame(this.playBuzz, this.pitch);
    }

    @Override
    public void onFrame() {
        this.playBuzz = this.emulator.getInterpreter().getST() > 0;
    }

    private record SampleFrame(boolean playBuzz, int pitch) implements AudioGenerator.SampleFrame {}

}
