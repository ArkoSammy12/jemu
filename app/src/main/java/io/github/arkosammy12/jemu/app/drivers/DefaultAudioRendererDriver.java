package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import io.github.arkosammy12.jemu.frontend.audio.AudioChannels;
import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.core.SpeedModeSettingChangedEvent;

import javax.sound.sampled.LineUnavailableException;
import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class DefaultAudioRendererDriver implements AudioDriver, Closeable {

    protected final Jemu jemu;
    protected final AudioGenerator audioGenerator;

    private final BlockingQueue<AudioGenerator.SampleFrame> sampleFrameBuffer = new ArrayBlockingQueue<>(2);
    private volatile boolean syncToAudio = true;

    public DefaultAudioRendererDriver(Jemu jemu, Emulator emulator) throws LineUnavailableException {
        this.jemu = jemu;
        this.audioGenerator = emulator.getAudioGenerator();
        this.jemu.getAudioEngine().setSampleFrameCallback(() -> {
            AudioGenerator.SampleFrame sampleFrame = this.sampleFrameBuffer.poll();
            if (sampleFrame == null) {
                return null;
            }
            return this.audioGenerator.getSampleFrameResampler().resample(this.getSampleRate(), this.getSamplesPerFrame(), sampleFrame).map(this::ensureBitDepth).orElse(null);
        });

        SpeedMode speedMode = jemu.getMainWindow().getConfigurations().getSettings().getSpeedSettings().getSpeedMode();
        this.jemu.getAudioEngine().setFramerate(speedMode.scaleFramerate(emulator.getFramerate()));
        this.jemu.getAudioEngine().setAudioChannels(this.audioGenerator.isStereo() ? AudioChannels.STEREO : AudioChannels.MONO);
        if (speedMode == SpeedMode.UNLIMITED) {
            this.syncToAudio = false;
        }
    }

    @Override
    public int getSampleRate() {
        return this.jemu.getAudioEngine().getSampleRate();
    }

    @Override
    public int getSamplesPerFrame() {
        return this.jemu.getAudioEngine().getSamplesPerFrame();
    }

    public void onSpeedModeSettingChanged(SpeedMode speedMode) {
        this.syncToAudio = speedMode != SpeedMode.UNLIMITED;
        this.clearAudioBuffer();
    }

    public void onFrame() {
        if (this.syncToAudio) {
            this.audioGenerator.getSampleFrame().ifPresent(sampleFrame -> {
                try {
                    this.sampleFrameBuffer.put(sampleFrame);
                } catch (InterruptedException _) {
                }
            });
        }
    }

    protected abstract byte[] ensureBitDepth(byte[] buf);

    private void clearAudioBuffer() {
        this.sampleFrameBuffer.clear();
    }

    @Override
    public void close() {
        this.clearAudioBuffer();
    }

}
