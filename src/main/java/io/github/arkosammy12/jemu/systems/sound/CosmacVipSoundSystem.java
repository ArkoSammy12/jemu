package io.github.arkosammy12.jemu.systems.sound;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.main.AudioRenderer;

public class CosmacVipSoundSystem implements SoundSystem {

    public static final int SQUARE_WAVE_AMPLITUDE = 4;

    protected final Jemu jemu;
    protected double step = (4000 * Math.pow(2.0, (175 - 64) / 48.0)) / 128.0 / SAMPLE_RATE;
    protected double phase = 0.0;

    private static final int[] DEFAULT_PATTERN_1 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] DEFAULT_PATTERN_2 = {
            0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0, 0, 0
    };

    public CosmacVipSoundSystem(Emulator emulator) {
        this.jemu = emulator.getEmulatorSettings().getJemu();
    }

    public void pushSamples(int soundTimer) {
        if (soundTimer <= 0) {
            this.phase = 0;
            return;
        }
        AudioRenderer audioRenderer = this.jemu.getAudioRenderer();
        byte[] data = new byte[audioRenderer.getSamplesPerFrame()];
        for (int i = 0; i < data.length; i++) {
            int bitStep = (int) (this.phase * 128);
            data[i] = (byte) (((DEFAULT_PATTERN_2[bitStep >> 3]) & (1 << (7 ^ (bitStep & 7)))) != 0 ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
            this.phase = (this.phase + step) % 1.0;
        }
        audioRenderer.pushSamples8(data);
    }

}