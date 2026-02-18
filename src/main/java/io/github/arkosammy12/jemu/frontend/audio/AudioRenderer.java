package io.github.arkosammy12.jemu.frontend.audio;

import java.io.Closeable;

public abstract class AudioRenderer implements Closeable {

    public static final int SAMPLE_RATE = 44100;

    protected final int framerate;

    public AudioRenderer(int framerate) {
        this.framerate = framerate;
    }

    public final int getSampleRate() {
        return SAMPLE_RATE;
    }

    public abstract int getSamplesPerFrame();

    public abstract int getBytesPerFrame();

    abstract public void setVolume(int volume);

    abstract public void setPaused(boolean paused);

    abstract public void setMuted(boolean muted);

    abstract public boolean needsFrame();

    abstract public void pushSampleFrame(byte[] samples);

}
