package io.github.arkosammy12.jemu.frontend.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.SampleRateChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.audio.VolumeChangedEvent;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.util.function.Supplier;

public class AudioEngine implements Closeable {

    private static final int TARGET_FRAME_LATENCY = 2;

    private int samplesPerFrame;
    private int bytesPerFrame;
    private byte[] emptySamples = new byte[0];

    private SourceDataLine currentSourceDataLine;
    private FloatControl volumeControl;
    private BooleanControl muteControl;

    private final Thread audioThread;
    private final Object audioThreadLock = new Object();
    private final Object currentLineLock = new Object();
    private volatile boolean running;
    private volatile boolean audioLineRunning;

    private AudioChannels audioChannels = AudioChannels.MONO;
    private SampleRate sampleRate = SampleRate.HZ_44100;
    private volatile int volume;
    private volatile int framerate;
    private volatile boolean paused = true;
    private volatile boolean muted;

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
        synchronized (this.currentLineLock) {
            this.sampleFrameCallback = sampleFrameCallback;
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setMuted(boolean muted) {
        synchronized (this.currentLineLock) {
            this.muted = muted;
            synchronized (this.currentLineLock) {
                if (this.muteControl != null) {
                    this.muteControl.setValue(muted);
                }
            }
        }
    }

    public void setVolume(int volume) {
        synchronized (this.currentLineLock) {
            this.volume = volume;
            synchronized (this.currentLineLock) {
                if (this.volumeControl != null) {
                    this.volumeControl.setValue(20.0f * (float) Math.log10(this.volume / 100.0));
                }
            }
        }
    }

    public void setFramerate(int framerate) throws LineUnavailableException {
        synchronized (this.currentLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.framerate = framerate;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setSampleRate(SampleRate sampleRate) throws LineUnavailableException {
        synchronized (this.currentLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.sampleRate = sampleRate;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public void setAudioChannels(AudioChannels audioChannels) throws LineUnavailableException {
        synchronized (this.currentLineLock) {
            boolean audioLineWasRunning = this.audioLineRunning;
            this.stop();
            this.audioChannels = audioChannels;
            this.recalculateFrameMetrics();
            if (audioLineWasRunning) {
                this.start();
            }
        }
    }

    public int getSampleRate() {
        return switch (this.sampleRate) {
            case HZ_44100 -> 44100;
            case HZ_48000 -> 48000;
        };
    }

    public int getSamplesPerFrame() {
        return this.samplesPerFrame;
    }

    public int getBytesPerFrame() {
        return this.bytesPerFrame;
    }

    public void onAudioSettingChanged(AudioSettingChangeEvent event) throws LineUnavailableException {
        switch (event) {
            case MuteEvent(boolean mute) -> this.setMuted(mute);
            case VolumeChangedEvent(int newVolume) -> this.setVolume(newVolume);
            case SampleRateChangedEvent(SampleRate newRate) -> this.setSampleRate(newRate);
            case null, default -> {}
        }
    }

    public void start() throws LineUnavailableException {
        synchronized (this.currentLineLock) {
            if (this.currentSourceDataLine != null) {
                this.stop();
            }

            AudioFormat format = new AudioFormat((float) this.getSampleRate(), 16, this.audioChannels == AudioChannels.STEREO ? 2 : 1, true, false);
            this.currentSourceDataLine = AudioSystem.getSourceDataLine(format);
            this.currentSourceDataLine.open(format, this.bytesPerFrame * (TARGET_FRAME_LATENCY + 1));
            this.volumeControl = (FloatControl) this.currentSourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            this.muteControl = (BooleanControl) this.currentSourceDataLine.getControl(BooleanControl.Type.MUTE);

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
        synchronized (this.currentLineLock) {
            if (this.currentSourceDataLine != null) {
                this.currentSourceDataLine.stop();
                this.currentSourceDataLine.flush();
                this.currentSourceDataLine.close();
                this.currentSourceDataLine = null;

                this.volumeControl = null;
                this.muteControl = null;
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
            if (this.audioLineRunning) {
                this.pushAudioFrame();
            }
        }
    }

    private void pushAudioFrame() {
        Supplier<byte[]> callback;
        synchronized (this.currentLineLock) {
            callback = this.sampleFrameCallback;
        }
        byte[] writtenSamples = callback == null ? this.emptySamples : callback.get();
        SourceDataLine line;
        synchronized (this.currentLineLock) {
            if (this.currentSourceDataLine == null) {
                return;
            }
            line = this.currentSourceDataLine;
            if (!this.audioLineFirstFrame) {
                this.audioLineFirstFrame = true;
                line.flush();
                line.start();
                writtenSamples = new byte[line.getBufferSize()];
            } else if (this.paused || writtenSamples == null) {
                writtenSamples = this.emptySamples;
            }
            writtenSamples = this.ensureBufferLength(writtenSamples);
        }
        line.write(writtenSamples, 0, writtenSamples.length);
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

        synchronized (this.currentLineLock) {
            if (this.currentSourceDataLine != null) {
                this.currentSourceDataLine.stop();
                this.currentSourceDataLine.flush();
                this.currentSourceDataLine.close();
                this.currentSourceDataLine = null;
            }
        }
    }

}