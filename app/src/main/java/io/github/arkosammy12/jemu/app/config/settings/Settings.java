package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;

public class Settings {

    @SerializedName("file")
    private final FileSettings fileSettings = new FileSettings();

    @SerializedName("emulator")
    private final EmulatorSettings emulatorSettings = new EmulatorSettings();

    @SerializedName("window")
    private final WindowSettings windowSettings = new WindowSettings();

    @SerializedName("video")
    private final VideoSettings videoSettings = new VideoSettings();

    @SerializedName("audio")
    private final AudioSettings audioSettings = new AudioSettings();

    @SerializedName("speed")
    private final SpeedSettings speedSettings = new SpeedSettings();

    public FileSettings getFileSettings() {
        return this.fileSettings;
    }

    public EmulatorSettings getEmulatorSettings() {
        return this.emulatorSettings;
    }

    public WindowSettings getWindowSettings() {
        return this.windowSettings;
    }

    public VideoSettings getVideoSettings() {
        return this.videoSettings;
    }

    public AudioSettings getAudioSettings() {
        return this.audioSettings;
    }

    public SpeedSettings getSpeedSettings() {
        return this.speedSettings;
    }

}
