package io.github.arkosammy12.jemu.application.audio;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.backend.common.AudioGenerator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import java.util.Optional;

public final class MonoAudioDriver extends AudioRenderer {

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

    public MonoAudioDriver(Jemu jemu, int frameRate) {
        super();
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
            this.setFramerate(frameRate);
            jemu.addEmulatorFrameListener((_, _) -> this.onFrame());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Source Data Line for audio", e);
        }
    }

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
    public void setFramerate(int framerate) {
        this.samplesPerFrame = SAMPLE_RATE / framerate;
        this.bytesPerFrame = this.samplesPerFrame * BYTES_PER_OUTPUT_SAMPLE;
        this.targetByteLatency = this.bytesPerFrame * TARGET_FRAME_LATENCY;
        this.emptySamples = new byte[this.bytesPerFrame];
    }

    @Override
    public int getSampleRate() {
        return SAMPLE_RATE;
    }

    @Override
    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    public void setVolume(int volume) {
        if (this.volumeControl != null) {
            this.volumeControl.setValue(20.0f * (float) Math.log10(Math.clamp(volume, 0, 100) / 100.0));
        }
    }

    private void onFrame() {
        if (!this.started) {
            byte[] prefill = new byte[this.audioLine.getBufferSize()];
            this.audioLine.flush();
            this.audioLine.write(prefill, 0, prefill.length);
            this.audioLine.start();
            this.started = true;
            return;
        }

        if (this.muted || this.paused || this.audioGenerator == null) {
            this.audioLine.write(this.emptySamples, 0, this.emptySamples.length);
            return;
        }

        byte[] writtenSamples = this.emptySamples;
        Optional<byte[]> optionalSamples = this.audioGenerator.getSampleFrame();
        if (optionalSamples.isPresent()) {
            writtenSamples =  this.resampleIfNecessary(optionalSamples.get(), this.audioGenerator);
        }
        writtenSamples = this.ensureBufferLength(writtenSamples);
        this.audioLine.write(writtenSamples, 0, writtenSamples.length);
    }

    private byte[] resampleIfNecessary(byte[] buf, AudioGenerator<?> audioGenerator) {
        return switch (audioGenerator.getBytesPerSample()) {
            case BYTES_1 -> {
                byte[] buf16 = new byte[this.bytesPerFrame];
                for (int i = 0; i < buf.length; i++) {
                    int sample16 = buf[i] * 256;
                    buf16[i * 2] = (byte) ((sample16 & 0xFF00) >>> 8);
                    buf16[(i * 2) + 1] = (byte) (sample16 & 0xFF);
                }
                yield buf16;
            }
            case BYTES_2 -> buf;
        };
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
