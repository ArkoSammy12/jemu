package io.github.arkosammy12.jemu.main;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import static io.github.arkosammy12.jemu.main.Main.MAIN_FRAMERATE;
import static io.github.arkosammy12.jemu.systems.sound.SoundSystem.SAMPLE_RATE;

import java.io.Closeable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

final class DefaultAudioRenderer implements AudioRenderer, Closeable {

    private static final int BYTES_PER_SAMPLE = 2;
    private static final int TARGET_FRAME_LATENCY = 3;

    private int samplesPerFrame;
    private int bytesPerFrame;
    private int targetByteLatency;
    private byte[] emptySamples;

    private final SourceDataLine audioLine;
    private final FloatControl volumeControl;
    private final Queue<byte[]> samples = new ConcurrentLinkedQueue<>();
    private boolean paused = true;
    private boolean muted = false;
    private boolean started = false;

    DefaultAudioRenderer(Jemu jemu) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BYTES_PER_SAMPLE * 8, 1, true, true);
            audioLine = AudioSystem.getSourceDataLine(format);
            audioLine.open(format);
            FloatControl control = null;
            if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                control = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(20.0f * (float) Math.log10(50 / 100.0));
            }
            this.volumeControl = control;
            this.setFramerate(MAIN_FRAMERATE);
            jemu.addEmulatorFrameListener(_ -> this.onFrame());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Source Data Line for audio", e);
        }
    }

    public boolean needsFrame() {
        return (this.audioLine.getBufferSize() - this.audioLine.available()) <= this.targetByteLatency;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    void setFramerate(int framerate) {
        this.samplesPerFrame = SAMPLE_RATE / framerate;
        this.bytesPerFrame = this.samplesPerFrame * BYTES_PER_SAMPLE;
        this.targetByteLatency = this.bytesPerFrame * TARGET_FRAME_LATENCY;
        this.emptySamples = new byte[this.bytesPerFrame];
    }

    @Override
    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    @Override
    public void pushSamples8(byte[] buf) {
        if (this.paused) {
            return;
        }
        if (buf.length != this.samplesPerFrame) {
            throw new IllegalArgumentException("Audio buffer sample size must be " + this.samplesPerFrame + "!");
        }
        byte[] buf16 = new byte[this.bytesPerFrame];
        for (int i = 0; i < buf.length; i++) {
            int sample16 = buf[i] * 256;
            buf16[i * 2] = (byte) ((sample16 & 0xFF00) >>> 8);
            buf16[(i * 2) + 1] = (byte) (sample16 & 0xFF);
        }
        this.samples.offer(buf16);
    }

    @Override
    public void pushSamples16(short[] buf) {
        if (this.paused) {
            return;
        }
        if (buf.length != this.samplesPerFrame) {
            throw new IllegalArgumentException("Audio buffer sample size must be " + this.samplesPerFrame + "!");
        }
        byte[] buf16 = new byte[this.bytesPerFrame];
        for (int i = 0; i < buf.length; i++) {
            int sample16 = buf[i];
            buf16[i * 2] = (byte) ((sample16 & 0xFF00) >>> 8);
            buf16[(i * 2) + 1] = (byte) (sample16 & 0xFF);
        }
        this.samples.offer(buf16);
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
        byte[] samples = this.samples.poll();
        if (samples == null || this.muted) {
            samples = this.emptySamples;
        }
        this.audioLine.write(samples, 0, samples.length);
    }

    public void close() {
        this.audioLine.stop();
        this.audioLine.flush();
        this.audioLine.close();
    }

}
