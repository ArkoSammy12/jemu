package io.github.arkosammy12.jemu.application.drivers;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import io.github.arkosammy12.jemu.backend.drivers.AudioDriver;
import io.github.arkosammy12.jemu.frontend.audio.AudioRenderer;

import java.io.Closeable;

public abstract class DefaultAudioRendererDriver implements AudioDriver, Closeable {

    protected final AudioGenerator<?> audioGenerator;
    protected final AudioRenderer audioRenderer;

    public DefaultAudioRendererDriver(Jemu jemu, AudioGenerator<?> audioGenerator, AudioRenderer audioRenderer) {
        this.audioGenerator = audioGenerator;
        this.audioRenderer = audioRenderer;
        jemu.addEmulatorFrameListener((_, _) -> this.onFrame());
    }

    public AudioRenderer getAudioRenderer() {
        return this.audioRenderer;
    }

    protected abstract void onFrame();

}
