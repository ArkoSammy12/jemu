package io.github.arkosammy12.jemu.core.chip8.audio;

import io.github.arkosammy12.jemu.core.chip8.MegaChipEmulator;
import io.github.arkosammy12.jemu.core.chip8.bus.Chip8Bus;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class MegaChipAudioGenerator<E extends MegaChipEmulator> extends AbstractChip8AudioGenerator<E> {

    private static final int[] DEFAULT_PATTERN_1 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] DEFAULT_PATTERN_2 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0
    };

    private static final int SQUARE_WAVE_AMPLITUDE = 4;

    private byte @Nullable [] audioBuffer;

    private int trackSampleRate;
    private int trackStart;
    private int trackSize;
    private boolean loop;
    private boolean isPlaying;
    private double trackPhase;

    private double megaOffPhase = 0.0;

    public MegaChipAudioGenerator(E emulator) {
        super(emulator);
    }

    public void playTrack(int trackSampleRate, int trackSize, boolean loop, int trackStart) {
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            return;
        }
        this.trackSampleRate= trackSampleRate;
        this.trackStart = trackStart;
        this.trackSize = trackSize;
        this.loop = loop;
        this.trackPhase = 0;
        this.isPlaying = true;
    }

    public void stopTrack() {
        if (!this.emulator.getInterpreter().isMegaModeEnabled()) {
            return;
        }
        this.trackSampleRate= 0;
        this.trackStart = 0;
        this.trackSize = 0;
        this.loop = false;
        this.trackPhase = 0;
        this.isPlaying = false;
    }

    @Override
    protected SampleFrameResampler createSampleFrameResampler() {
        return (_, outputSamplesPerFrame, inputSampleFrame) -> {
            if (inputSampleFrame instanceof SampleFrame(byte @Nullable [] frameAudioBuffer)) {
                byte[] out = new byte[outputSamplesPerFrame];
                if (frameAudioBuffer != null) {
                    System.arraycopy(frameAudioBuffer, 0, out, 0, Math.min(out.length, frameAudioBuffer.length));
                }
                return Optional.of(out);
            } else {
                return Optional.empty();
            }
        };
    }

    @Override
    protected AudioGenerator.SampleFrame createSampleFrame() {
        return new SampleFrame(this.audioBuffer == null ? null : Arrays.copyOf(this.audioBuffer, this.audioBuffer.length));
    }

    @Override
    public void onFrame() {
        Optional<? extends AudioDriver> optionalAudioDriver = this.emulator.getHost().getAudioDriver();
        if (optionalAudioDriver.isEmpty()) {
            this.audioBuffer = null;
            return;
        }
        AudioDriver audioDriver = optionalAudioDriver.get();
        if (this.emulator.getInterpreter().isMegaModeEnabled()) {
            if (!this.isPlaying) {
                this.audioBuffer = null;
            } else {
                Chip8Bus bus = this.emulator.getBus();
                this.audioBuffer = new byte[audioDriver.getSamplesPerFrame()];
                double step = (double) this.trackSampleRate / audioDriver.getSampleRate();
                for (int i = 0; i < this.audioBuffer.length; i++) {
                    if (loop && this.trackPhase >= this.trackSize) {
                        this.trackPhase %= this.trackSize;
                    }
                    if (this.trackPhase < this.trackSize) {
                        this.audioBuffer[i] = (byte) (bus.readByte((int) (this.trackStart + this.trackPhase)) - 128);
                        this.trackPhase += step;
                    } else {
                        this.audioBuffer[i] = 0;
                    }
                }
            }
        } else if (this.emulator.getInterpreter().getST() > 0) {
            this.audioBuffer = new byte[audioDriver.getSamplesPerFrame()];
            double step = (4000 * Math.pow(2.0, (175 - 64) / 48.0)) / 128.0 / audioDriver.getSampleRate();
            for (int i = 0; i < this.audioBuffer.length; i++) {
                int bitStep = (int) (this.megaOffPhase * 128);
                this.audioBuffer[i] = (byte) (((DEFAULT_PATTERN_2[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
                this.megaOffPhase = (this.megaOffPhase + step) % 1.0;
            }
        } else {
            this.megaOffPhase = 0;
            this.audioBuffer = null;
        }
    }

    private record SampleFrame(byte @Nullable [] frameAudioBuffer) implements AudioGenerator.SampleFrame {}

}
