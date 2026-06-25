package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.frontend.audio.AudioChannels;

import javax.sound.sampled.LineUnavailableException;
import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class DefaultAudioRendererDriver implements AudioDriver, Closeable {

    protected final Jemu jemu;
    protected final AudioGenerator audioGenerator;

    private final ArrayBlockingQueue<byte[]> audioBuffer = new ArrayBlockingQueue<>(2);

    public DefaultAudioRendererDriver(Jemu jemu, Emulator emulator) throws LineUnavailableException {
        this.jemu = jemu;
        this.audioGenerator = emulator.getAudioGenerator();
        this.jemu.getAudioEngine().setSampleFrameCallback(this.audioBuffer::poll);
        this.jemu.getAudioEngine().setFramerate(emulator.getFramerate());
        this.jemu.getAudioEngine().setAudioChannels(this.audioGenerator.isStereo() ? AudioChannels.STEREO : AudioChannels.MONO);
    }

    @Override
    public int getSampleRate() {
        return this.jemu.getAudioEngine().getSampleRate();
    }

    @Override
    public int getSamplesPerFrame() {
        return this.jemu.getAudioEngine().getSamplesPerFrame();
    }

    public void onFrame() {
        this.audioGenerator.getSampleFrame().map(this::convertBitDepthIfNecessary).ifPresent(samples -> {
            try {
                this.audioBuffer.put(samples);
            } catch (InterruptedException _) {}
        });
    }

    protected abstract byte[] convertBitDepthIfNecessary(byte[] buf);

    @Override
    public void close() {
        this.audioBuffer.clear();
    }

}
