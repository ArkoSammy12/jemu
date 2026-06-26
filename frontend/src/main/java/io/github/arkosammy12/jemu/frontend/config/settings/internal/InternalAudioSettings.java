package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.audio.SampleRate;
import io.github.arkosammy12.jemu.frontend.config.settings.AudioSettings;
import org.jetbrains.annotations.NotNull;

public class InternalAudioSettings implements AudioSettings {

    @SerializedName("volume")
    private volatile int volume = 50;

    @SerializedName("mute")
    private volatile boolean mute = false;

    @SerializedName("sample_rate")
    private volatile SampleRate sampleRate = SampleRate.HZ_44100;

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    @Override
    public boolean getMute() {
        return this.mute;
    }

    public void setSampleRate(@NotNull SampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public SampleRate getSampleRate() {
        return this.sampleRate;
    }

}
