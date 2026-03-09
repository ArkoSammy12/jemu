package io.github.arkosammy12.jemu.frontend.audio;

import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.Closeable;

public abstract class AudioRenderer implements Closeable {

    public static final int SAMPLE_RATE = 44100;
    protected static final int TARGET_FRAME_LATENCY = 3;

    protected final int samplesPerFrame;
    protected final int bytesPerFrame;
    protected final int targetByteLatency;
    protected final byte[] emptySamples;

    protected final SourceDataLine audioLine;
    protected final int framerate;
    protected final FloatControl volumeControl;
    protected boolean paused = true;
    protected boolean muted = false;
    protected boolean started = false;

    public AudioRenderer(int framerate) {
        this.framerate = framerate;
        try {
            AudioFormat format = this.getAudioFormat();
            audioLine = AudioSystem.getSourceDataLine(format);
            audioLine.open(format);
            FloatControl control = null;
            if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(20.0f * (float) Math.log10(50 / 100.0));
            }

            this.volumeControl = control;
            this.samplesPerFrame = SAMPLE_RATE / framerate;
            this.bytesPerFrame = this.samplesPerFrame * this.getBytesPerOutputSample();
            this.targetByteLatency = this.bytesPerFrame * TARGET_FRAME_LATENCY;
            this.emptySamples = new byte[this.bytesPerFrame];
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Source Data Line for audio", e);
        }
    }

    public final int getSampleRate() {
        return SAMPLE_RATE;
    }

    public boolean needsFrame() {
        return (this.audioLine.getBufferSize() - this.audioLine.available()) <= this.targetByteLatency;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    public int getBytesPerFrame() {
        return this.bytesPerFrame;
    }

    public void setVolume(int volume) {
        if (this.volumeControl != null) {
            this.volumeControl.setValue(20.0f * (float) Math.log10(Math.clamp(volume, 0, 100) / 100.0));
        }
    }

    abstract protected AudioFormat getAudioFormat();

    abstract protected int getBytesPerOutputSample();

    public void pushSampleFrame(byte @Nullable [] samples) {
        if (!this.started) {
            //byte[] prefill = new byte[this.bytesPerFrame];
            this.audioLine.flush();
            //this.audioLine.write(prefill, 0, prefill.length);
            this.audioLine.start();
            this.started = true;
            //return;
        }

        if (this.muted || this.paused) {
            this.audioLine.write(this.emptySamples, 0, this.emptySamples.length);
            return;
        }

        byte[] writtenSamples = this.emptySamples;
        if (samples != null) {
            writtenSamples = samples;
        }
        writtenSamples = this.ensureBufferLength(writtenSamples);
        this.audioLine.write(writtenSamples, 0, writtenSamples.length);
    }

    abstract protected byte[] ensureBufferLength(byte[] buf);

    public void close() {
        this.audioLine.stop();
        this.audioLine.flush();
        this.audioLine.close();
    }

}
