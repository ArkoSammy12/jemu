package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.audio.SampleRate;
import io.github.arkosammy12.jemu.app.audio.SoundDevice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AudioSettings {

    @SerializedName("volume")
    private volatile int volume = 50;

    @SerializedName("mute")
    private volatile boolean mute = false;

    @SerializedName("sample_rate")
    private volatile SampleRate sampleRate = SampleRate.HZ_44100;

    @SerializedName("latency_ms")
    private volatile int latencyMs = 50;

    @SerializedName("sound_device")
    private volatile SoundDevice soundDevice;

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return this.volume;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean getMute() {
        return this.mute;
    }

    public void setSampleRate(@NotNull SampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

    public SampleRate getSampleRate() {
        return this.sampleRate;
    }

    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }

    public int getLatencyMs() {
        return this.latencyMs;
    }

    public void setSoundDevice(@Nullable SoundDevice soundDevice) {
        this.soundDevice = soundDevice;
    }

    public Optional<SoundDevice> getSoundDevice() {
        return Optional.ofNullable(this.soundDevice);
    }

}
