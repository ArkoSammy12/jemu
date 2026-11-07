package io.github.arkosammy12.jemu.frontend.audio;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.Mixer;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SoundDevice {

    @Nullable
    private final transient Mixer.Info mixerInfo;

    @SerializedName("name")
    private final String name;

    @SerializedName("vendor")
    private final String vendor;

    @SerializedName("description")
    private final String description;

    @SerializedName("version")
    private final String version;

    public SoundDevice(Mixer.Info mixerInfo) {
        this.mixerInfo = mixerInfo;
        this.name = mixerInfo.getName();
        this.vendor = mixerInfo.getVendor();
        this.description = mixerInfo.getDescription();
        this.version = mixerInfo.getVersion();
    }

    // Used by Gson during deserialization
    @SuppressWarnings("unused")
    private SoundDevice() {
        this.mixerInfo = null;
        this.name = null;
        this.vendor = null;
        this.description = null;
        this.version = null;
    }

    public String getName() {
        return this.name;
    }

    public boolean matches(@Nullable SoundDevice other) {
        return other != null
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.vendor, other.vendor)
                && Objects.equals(this.description, other.description)
                && Objects.equals(this.version, other.version);
    }

    public Optional<Mixer.Info> toMixerInfo() {
        return Optional.ofNullable(this.mixerInfo).or(() -> AudioLine.getAvailableSourceLineMixers().stream().filter(this::matchesMixerInfo).findFirst());
    }

    public static Collection<SoundDevice> getAvailableSoundDevices() {
        return AudioLine.getAvailableSourceLineMixers().stream().map(SoundDevice::new).collect(Collectors.toList());
    }

    private boolean matchesMixerInfo(Mixer.Info mixerInfo) {
        return Objects.equals(mixerInfo.getName(), this.name)
                && Objects.equals(mixerInfo.getVendor(), this.vendor)
                && Objects.equals(mixerInfo.getDescription(), this.description)
                && Objects.equals(mixerInfo.getVersion(), this.version);
    }

}
