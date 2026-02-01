package io.github.arkosammy12.jemu.systems.cosmacvip;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.main.AudioRenderer;
import io.github.arkosammy12.jemu.systems.SoundSystem;

import static io.github.arkosammy12.jemu.systems.cosmacvip.CosmacVipSoundSystem.SQUARE_WAVE_AMPLITUDE;

public class VP595 implements SoundSystem, IODevice {

    private final Jemu jemu;

    private double frequencyLatch = 27535.0 / (0x80 + 1);
    private double phase = 0.0;

    public VP595(Emulator emulator) {
        this.jemu = emulator.getEmulatorSettings().getJchip();
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
    public void pushSamples(int soundTimer) {
        double frequency = frequencyLatch;
        if (soundTimer <= 0) {
            phase = 0;
            return;
        }
        AudioRenderer audioRenderer = this.jemu.getAudioRenderer();
        byte[] data = new byte[audioRenderer.getSamplesPerFrame()];
        double step = frequency / SAMPLE_RATE;
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ((phase < 0.5) ? SQUARE_WAVE_AMPLITUDE : -SQUARE_WAVE_AMPLITUDE);
            phase = (phase + step) % 1;
        }
        audioRenderer.pushSamples8(data);
    }

}
