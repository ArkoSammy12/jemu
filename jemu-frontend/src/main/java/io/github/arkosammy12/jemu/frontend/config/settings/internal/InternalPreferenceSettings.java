package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.config.settings.*;

public class InternalPreferenceSettings implements PreferenceSettings {

    @SerializedName("file")
    private final InternalFileSettings fileSettings = new InternalFileSettings();

    @SerializedName("emulator")
    private final InternalEmulatorSettings emulatorSettings = new InternalEmulatorSettings();

    @SerializedName("window")
    private final InternalWindowSettings windowSettings = new InternalWindowSettings();

    @SerializedName("video")
    private final InternalVideoSettings videoSettings = new InternalVideoSettings();

    @SerializedName("audio")
    private final InternalAudioSettings audioSettings = new InternalAudioSettings();

    @SerializedName("speed")
    private final InternalSpeedSettings speedSettings = new InternalSpeedSettings();

    public InternalFileSettings getInternalFileSettings() {
        return this.fileSettings;
    }

    public InternalEmulatorSettings getInternalEmulatorSettings() {
        return this.emulatorSettings;
    }

    @Override
    public WindowSettings getWindowSettings() {
        return this.windowSettings;
    }

    public InternalWindowSettings getInternalWindowSettings() {
        return this.windowSettings;
    }

    @Override
    public VideoSettings getVideoSettings() {
        return this.videoSettings;
    }

    public InternalVideoSettings getInternalVideoSettings() {
        return this.videoSettings;
    }

    @Override
    public AudioSettings getAudioSettings() {
        return this.audioSettings;
    }

    public InternalAudioSettings getInternalAudioSettings() {
        return this.audioSettings;
    }

    @Override
    public SpeedSettings getSpeedSettings() {
        return this.speedSettings;
    }

    public InternalSpeedSettings getInternalSpeedSettings() {
        return this.speedSettings;
    }

}
