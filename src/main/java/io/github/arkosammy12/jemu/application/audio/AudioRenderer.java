package io.github.arkosammy12.jemu.application.audio;

import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public abstract class AudioRenderer implements AudioDriver, Closeable {

    public static final int SAMPLE_RATE = 44100;

    protected @Nullable AudioGenerator<?> audioGenerator;

    public void setAudioGenerator(@Nullable AudioGenerator<?> audioGenerator) {
        this.audioGenerator = audioGenerator;
    }

    abstract public void setFramerate(int framerate);

    abstract public void setVolume(int volume);

    abstract public void setPaused(boolean paused);

    abstract public void setMuted(boolean muted);

    abstract public boolean needsFrame();

}
