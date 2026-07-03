package io.github.arkosammy12.jemu.frontend.audio;

import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;

public class AudioLine implements Closeable {

    private final AudioFormat audioFormat;
    private final SourceDataLine sourceDataLine;

    @Nullable
    private FloatControl volumeControl;

    @Nullable
    private BooleanControl muteControl;

    private volatile int volume;
    private volatile boolean muted;

    public AudioLine(AudioFormat audioFormat) throws LineUnavailableException {
        this.audioFormat = audioFormat;
        this.sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        this.volumeControl = this.createVolumeControl();
        this.muteControl = this.createMuteControl();
    }

    public AudioLine(AudioFormat audioFormat, Mixer.Info mixerInfo) throws LineUnavailableException {
        this.audioFormat = audioFormat;
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        this.sourceDataLine = (SourceDataLine) mixer.getLine(lineInfo);
        this.volumeControl = this.createVolumeControl();
        this.muteControl = this.createMuteControl();
    }

    public void open() throws LineUnavailableException {
        this.sourceDataLine.open();
        this.volumeControl = this.createVolumeControl();
        this.muteControl = this.createMuteControl();
    }

    public void open(int bufferSize) throws LineUnavailableException {
        this.sourceDataLine.open(this.audioFormat, bufferSize);
        this.volumeControl = this.createVolumeControl();
        this.muteControl = this.createMuteControl();
    }

    public void setVolume(int volume) {
        volume = Math.clamp(volume, 0, 100);
        this.volume = volume;
        if (this.volumeControl != null) {
            this.volumeControl.setValue(20.0f * (float) Math.log10(this.volume / 100.0));
        }
    }

    public void setMuted(boolean mute) {
        this.muted = mute;
        if (this.muteControl != null) {
            this.muteControl.setValue(muted);
        }
    }

    public int getBufferSize() {
        return this.sourceDataLine.getBufferSize();
    }

    public void flushAndStart() {
        this.sourceDataLine.flush();
        this.sourceDataLine.start();
    }

    public void stopAndFlush() {
        this.sourceDataLine.stop();
        this.sourceDataLine.flush();
    }

    public void write(byte[] buf) {
        if (this.muteControl == null && this.muted) {
            buf = new byte[buf.length];
        }
        this.sourceDataLine.write(buf, 0, buf.length);
    }

    @Override
    public void close() {
        if (this.sourceDataLine != null) {
            this.stopAndFlush();
            this.sourceDataLine.close();
        }
    }

    private FloatControl createVolumeControl() {
        if (this.sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return (FloatControl) this.sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
        } else {
            return null;
        }
    }

    private BooleanControl createMuteControl() {
        if (this.sourceDataLine.isControlSupported(BooleanControl.Type.MUTE)) {
            return (BooleanControl) this.sourceDataLine.getControl(BooleanControl.Type.MUTE);
        } else {
            return null;
        }
    }

    public static Collection<Mixer.Info> getAvailableSourceLineMixers() {
        Line.Info lineInfo = new Line.Info(SourceDataLine.class);
        Collection<Mixer.Info> availableMixers = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (AudioSystem.getMixer(info).isLineSupported(lineInfo)) {
                availableMixers.add(info);
            }
        }
        return availableMixers;
    }

}
