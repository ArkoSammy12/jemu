package io.github.arkosammy12.jemu.systems;

public interface SoundSystem {

    int SAMPLE_RATE = 44100;

    void pushSamples(int soundTimer);

}
