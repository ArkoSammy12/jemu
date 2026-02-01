package io.github.arkosammy12.jemu.main;

public interface AudioRenderer {

    void pushSamples8(byte[] samples);

    void pushSamples16(short[] samples);

    void setVolume(int volume);

    void setMuted(boolean muted);

    int getSamplesPerFrame();

}
