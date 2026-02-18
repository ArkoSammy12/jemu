package io.github.arkosammy12.jemu.frontend.audio;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import java.util.Optional;

public final class MonoAudioRenderer extends AudioRenderer {

    private static final int BYTES_PER_OUTPUT_SAMPLE = 2;
    private static final int TARGET_FRAME_LATENCY = 3;

    private int samplesPerFrame;
    private int bytesPerFrame;
    private int targetByteLatency;
    private byte[] emptySamples;

    private final SourceDataLine audioLine;
    private final FloatControl volumeControl;
    private boolean paused = true;
    private boolean muted = false;
    private boolean started = false;

    public MonoAudioRenderer(int frameRate) {
        super(frameRate);
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BYTES_PER_OUTPUT_SAMPLE * 8, 1, true, true);
            audioLine = AudioSystem.getSourceDataLine(format);
            audioLine.open(format);
            FloatControl control = null;
            if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(20.0f * (float) Math.log10(50 / 100.0));
            }

            this.volumeControl = control;
            this.samplesPerFrame = SAMPLE_RATE / framerate;
            this.bytesPerFrame = this.samplesPerFrame * BYTES_PER_OUTPUT_SAMPLE;
            this.targetByteLatency = this.bytesPerFrame * TARGET_FRAME_LATENCY;
            this.emptySamples = new byte[this.bytesPerFrame];

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Source Data Line for audio", e);
        }
    }

    @Override
    public boolean needsFrame() {
        return (this.audioLine.getBufferSize() - this.audioLine.available()) <= this.targetByteLatency;
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
    }


    @Override
    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    @Override
    public int getBytesPerFrame() {
        return this.bytesPerFrame;
    }

    @Override
    public void setVolume(int volume) {
        if (this.volumeControl != null) {
            this.volumeControl.setValue(20.0f * (float) Math.log10(Math.clamp(volume, 0, 100) / 100.0));
        }
    }

    @Override
    public void pushSampleFrame(byte @Nullable [] samples) {
        if (!this.started) {
            byte[] prefill = new byte[this.audioLine.getBufferSize()];
            this.audioLine.flush();
            this.audioLine.write(prefill, 0, prefill.length);
            this.audioLine.start();
            this.started = true;
            return;
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

    private byte[] ensureBufferLength(byte[] buf) {
        if (buf.length == this.bytesPerFrame) {
            return buf;
        }
        byte[] actualBuf = new byte[this.bytesPerFrame];
        System.arraycopy(buf, 0, actualBuf, 0, buf.length);

        byte lastSample = buf[buf.length - 1];
        for (int i = buf.length; i < actualBuf.length; i++) {
            actualBuf[i] = lastSample;
        }
        return actualBuf;
    }

    public void close() {
        this.audioLine.stop();
        this.audioLine.flush();
        this.audioLine.close();
    }

}
