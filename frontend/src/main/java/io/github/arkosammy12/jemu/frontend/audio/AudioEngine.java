package io.github.arkosammy12.jemu.frontend.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.SoundDeviceChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.VolumeChangedEvent;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.util.Optional;
import java.util.function.Supplier;

public class AudioEngine implements Closeable {

    private static final int TARGET_FRAME_LATENCY = 2;

    private int samplesPerFrame;
    private int bytesPerFrame;
    private byte[] emptySamples = new byte[0];

    private AudioLine audioLine;

    private final Thread audioThread;
    private final Object audioThreadLock = new Object();
    private final Object audioLineLock = new Object();
    private volatile boolean running;
    private volatile boolean audioLineRunning;

    @Nullable
    private SoundDevice soundDevice;
    private AudioChannels audioChannels = AudioChannels.MONO;
    private SampleRate sampleRate = SampleRate.HZ_44100;
    private volatile int volume;
    private volatile int framerate;
    private volatile boolean muted;

    private volatile boolean paused = true;
    private boolean audioLineFirstFrame;

    private volatile Supplier<byte[]> sampleFrameCallback;

    public AudioEngine(String threadName) throws LineUnavailableException {
        this.running = true;

        this.audioThread = new Thread(this::audioLoop, threadName);
        this.audioThread.setDaemon(true);
        this.audioThread.start();

        this.setFramerate(60);
        this.setVolume(50);
    }

    public void setSampleFrameCallback(Supplier<byte[]> sampleFrameCallback) {
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
            case SoundDeviceChangedEvent(SoundDevice newSoundDevice) -> this.soundDevice(newSoundDevice);
            case SampleRateChangedEvent(SampleRate newRate) -> this.setSampleRate(newRate);
            case MuteEvent(boolean mute) -> this.setMuted(mute);
            case VolumeChangedEvent(int newVolume) -> this.setVolume(newVolume);
            case null, default -> {}
        }
    }

    public void start() throws LineUnavailableException {
        synchronized (this.audioLineLock) {
            if (this.audioLine != null) {
                this.stop();
            }

            AudioFormat format = new AudioFormat((float) this.getSampleRate(), 16, this.audioChannels == AudioChannels.STEREO ? 2 : 1, true, false);

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
            } else if (this.paused || writtenSamples == null) {
                writtenSamples = this.emptySamples;
            }
            writtenSamples = this.ensureBufferLength(writtenSamples);
        }
        line.write(writtenSamples);
    }

    private int getBytesPerOutputSample() {
        return switch (this.audioChannels) {
            case MONO -> 2;
            case STEREO -> 4;
        };
    }

    private byte[] ensureBufferLength(byte[] buf) {
        if (buf.length == this.bytesPerFrame) {
            return buf;
        }
        byte[] actualBuf = new byte[this.bytesPerFrame];
        int copyLength = Math.min(buf.length, this.bytesPerFrame);
        System.arraycopy(buf, 0, actualBuf, 0, copyLength);
        if (copyLength < this.bytesPerFrame) {
            int frameSize = this.getBytesPerOutputSample();
            int alignedLength = (copyLength / frameSize) * frameSize;
            for (int i = alignedLength; i < actualBuf.length; i += frameSize) {
                System.arraycopy(buf, alignedLength - frameSize, actualBuf, i, frameSize);
            }
        }
        return actualBuf;
    }

    private void recalculateFrameMetrics() {
        this.samplesPerFrame = this.getSampleRate() / this.framerate;
        this.bytesPerFrame = this.samplesPerFrame * this.getBytesPerOutputSample();
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