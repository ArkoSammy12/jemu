package io.github.arkosammy12.jemu.frontend.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.SoundDeviceChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.VolumeChangedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.util.Optional;
import java.util.function.Supplier;

public class AudioEngine implements Closeable {

    private static final int BYTES_PER_SAMPLE = 2;
    private static final boolean SIGNED_SAMPLES = true;
    private static final boolean BIG_ENDIAN_SAMPLES = false;
    private static final int TARGET_FRAME_LATENCY = 2;

    private volatile AudioLine audioLine;
    private volatile Supplier<byte @Nullable []> sampleFrameCallback;

    @Nullable
    private SoundDevice soundDevice;
    private AudioChannels audioChannels = AudioChannels.MONO;
    private SampleRate sampleRate = SampleRate.HZ_44100;
    private volatile boolean muted;
    private volatile int volume;
    private volatile int framerate;
    private volatile boolean paused;

    private volatile int samplesPerFrame;
    private volatile int bytesPerFrame;
    private volatile byte[] emptySamples = new byte[0];

    private final Thread audioThread;
    private final Object audioThreadLock = new Object();
    private final Object audioLineLock = new Object();
    private volatile boolean running;
    private volatile boolean audioLineRunning;

    private boolean audioLineFirstFrame;
    private byte @Nullable [] lastWrittenBuffer;

    public AudioEngine(String threadName) throws LineUnavailableException {
        this.running = true;

        this.audioThread = new Thread(this::audioLoop, threadName);
        this.audioThread.setDaemon(true);
        this.audioThread.start();

        this.setFramerate(60);
        this.setVolume(50);
    }

    public void setSampleFrameCallback(Supplier<byte @Nullable []> sampleFrameCallback) {
        synchronized (this.audioLineLock) {
            this.sampleFrameCallback = sampleFrameCallback;
        }
    }

    public void soundDevice(@Nullable SoundDevice soundDevice) throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.soundDevice = soundDevice;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setAudioChannels(AudioChannels audioChannels) throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.audioChannels = audioChannels;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setSampleRate(SampleRate sampleRate) throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.sampleRate = sampleRate;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setMuted(boolean muted) {
        synchronized (this.audioLineLock) {
            this.muted = muted;
            if (this.audioLine != null) {
                this.audioLine.setMuted(muted);
            }
        }
    }

    public void setVolume(int volume) {
        synchronized (this.audioLineLock) {
            this.volume = volume;
            if (this.audioLine != null) {
                this.audioLine.setVolume(volume);
            }
        }
    }

    public void setFramerate(int framerate) throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.framerate = framerate;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getSampleRate() {
        return this.sampleRate.getIntValue();
    }

    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    public int getBytesPerFrame() {
        return this.bytesPerFrame;
    }

    public void onAudioSettingChanged(AudioSettingChangeEvent event) throws LineUnavailableException {
        switch (event) {
            case SoundDeviceChangedEvent soundDeviceChangedEvent -> this.soundDevice(soundDeviceChangedEvent.getSoundDevice().orElse(null));
            case SampleRateChangedEvent sampleRateChangedEvent -> this.setSampleRate(sampleRateChangedEvent.getSampleRate());
            case MuteEvent muteEvent -> this.setMuted(muteEvent.getMute());
            case VolumeChangedEvent volumeChangedEvent -> this.setVolume(volumeChangedEvent.getNewVolume());
            case null, default -> {}
        }
    }

    public void start() throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            if (this.audioLine != null) {
                this.stop();
            }

            AudioFormat format = new AudioFormat((float) this.getSampleRate(), BYTES_PER_SAMPLE * 8, this.audioChannels.getChannelCount(), SIGNED_SAMPLES, BIG_ENDIAN_SAMPLES);

            if (this.soundDevice == null) {
                this.audioLine = new AudioLine(format);
            } else {
                Optional<Mixer.Info> mixerInfo = this.soundDevice.toMixerInfo();
                if (mixerInfo.isEmpty()) {
                    this.audioLine = new AudioLine(format);
                } else {
                    this.audioLine = new AudioLine(format, mixerInfo.get());
                }
            }

            this.audioLine.open(this.bytesPerFrame * (TARGET_FRAME_LATENCY + 1));

            this.setVolume(this.volume);
            this.setMuted(this.muted);

            this.audioLineFirstFrame = false;
            this.audioLineRunning = true;
            this.lastWrittenBuffer = null;
        }

        synchronized (this.audioThreadLock) {
            this.audioThreadLock.notify();
        }
    }

    public void stop() {
        synchronized (this.audioLineLock) {
            if (this.audioLine != null) {
                this.audioLine.close();
                this.audioLine = null;
                this.audioLineRunning = false;
                this.audioLineFirstFrame = false;
                this.lastWrittenBuffer = null;
            }
        }
    }

    private void audioLoop() {
        while (this.running) {
            synchronized (this.audioThreadLock) {
                if (!this.audioLineRunning) {
                    try {
                        this.audioThreadLock.wait();
                    } catch (InterruptedException _) {}
                }
            }
            if (this.running && this.audioLineRunning) {
                this.pushAudioFrame();
            }
        }
    }

    private void pushAudioFrame() {
        Supplier<byte[]> callback;
        synchronized (this.audioLineLock) {
            callback = this.sampleFrameCallback;
        }
        byte[] writtenSamples = callback == null ? this.emptySamples : callback.get();
        AudioLine line;
        synchronized (this.audioLineLock) {
            if (this.audioLine == null) {
                return;
            }
            line = this.audioLine;
            if (!this.audioLineFirstFrame) {
                this.audioLineFirstFrame = true;
                line.flushAndStart();
                writtenSamples = new byte[line.getBufferSize()];
            } else if (this.paused) {
                writtenSamples = this.emptySamples;
            }
            writtenSamples = this.ensureBufferLength(writtenSamples);
        }
        line.write(writtenSamples);
    }

    private int getBytesPerSample() {
        return this.audioChannels.getChannelCount() * BYTES_PER_SAMPLE;
    }

    private byte @NotNull [] ensureBufferLength(byte @Nullable [] buf) {
        byte[] ret;
        if (buf == null) {
            if (this.lastWrittenBuffer == null) {
                // If no cached previous sample frame, send a full frame of silence
                ret = this.emptySamples;
            } else {
                // If we have a cached sample frame, then create a new sample frame by repeating the last sample of the previous frame
                int bytesPerSample = this.getBytesPerSample();
                int lastSampleBeginIndex = Math.max(0, this.lastWrittenBuffer.length - bytesPerSample);
                ret = new byte[this.getBytesPerFrame()];
                for (int i = 0; i < ret.length; i += bytesPerSample) {
                    System.arraycopy(this.lastWrittenBuffer, lastSampleBeginIndex, ret, i, bytesPerSample);
                }
            }
        } else if (buf.length == this.getBytesPerFrame()) {
            // If the lengths match, we just return the buffer
            ret = buf;
        } else {
            // If the lengths don't match, create a new sample frame with the intended length
            ret = new byte[this.getBytesPerFrame()];

            // Copy the contents of the original buffer to the new buffer, truncating if attempting to write more bytes than
            // necessary
            int copyLength = Math.min(buf.length, ret.length);
            System.arraycopy(buf, 0, ret, 0, copyLength);
            if (copyLength < ret.length) {
                int bytesPerSample = this.getBytesPerSample();

                // Determine how far we are along we are in the middle of a possible cutoff sample made up of multiple bytes
                int sampleByteOffset = buf.length % bytesPerSample;

                if (this.lastWrittenBuffer == null) {
                    // If no cached previous sample frame, then first determine where to begin completing the sample frame
                    int paddingBeginOffset = buf.length - sampleByteOffset;

                    // Then, determine the index of the last fully formed, valid sample
                    int lastValidSampleBeginOffset = Math.max(0, paddingBeginOffset - bytesPerSample);

                    // Repeat the last sample across the rest of the returned sample buffer, overwriting the partially formed sample if any
                    for (int i = paddingBeginOffset; i < ret.length; i += bytesPerSample) {
                        System.arraycopy(buf, lastValidSampleBeginOffset, ret, i, bytesPerSample);
                    }

                } else {
                    // If we have a cached previous sample frame, then first determine how many bytes are needed to complete the last sample of the original buffer
                    int misalignedBytes = bytesPerSample - sampleByteOffset;

                    // Then, determine the index where to begin repeating the last written sample
                    int paddingBeginOffset = buf.length + misalignedBytes;

                    // Then, calculate the index of the last written sample in the previous sample frame buffer
                    int lastSampleBeginIndex = Math.max(0, this.lastWrittenBuffer.length - bytesPerSample);

                    // Complete the possibly cutoff sample by filling out the bytes with the corresponding bytes from the last written sample
                    for (int i = buf.length; i < paddingBeginOffset; i++) {
                        ret[i] = this.lastWrittenBuffer[lastSampleBeginIndex + sampleByteOffset + (i - buf.length)];
                    }

                    // Then, for the remaining length, repeat the entire last written sample
                    for (int i = paddingBeginOffset; i < ret.length; i += bytesPerSample) {
                        System.arraycopy(this.lastWrittenBuffer, lastSampleBeginIndex, ret, i, bytesPerSample);
                    }
                }
            }

        }
        this.lastWrittenBuffer = ret;
        return ret;
    }

    private void recalculateFrameMetrics() {
        this.samplesPerFrame = this.getSampleRate() / this.framerate;
        this.bytesPerFrame = this.samplesPerFrame * this.getBytesPerSample();
        this.emptySamples = new byte[this.bytesPerFrame];
    }

    @Override
    public void close() {
        this.running = false;
        this.audioLineRunning = false;

        synchronized (this.audioThreadLock) {
            this.audioThreadLock.notifyAll();
        }

        if (this.audioThread != null && !Thread.currentThread().equals(this.audioThread) && this.audioThread.isAlive()) {
            try {
                this.audioThread.join();
            } catch (InterruptedException _) {}
        }

        this.stop();
    }

}