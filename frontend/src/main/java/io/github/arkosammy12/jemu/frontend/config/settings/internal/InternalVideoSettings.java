package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class InternalVideoSettings implements VideoSettings {

    @SerializedName("use_integer_scanling")
    private volatile boolean useIntegerScaling = false;

    @SerializedName("aspect_ratio")
    private volatile AspectRatio aspectRatio = AspectRatio.AUTO;

    @Nullable
    @SerializedName("video_size")
    private volatile VideoSize videoSize = null;

    public void setUseIntegerScaling(boolean useIntegerScaling) {
        this.useIntegerScaling = useIntegerScaling;
    }

    @Override
    public boolean getUseIntegerScaling() {
        return this.useIntegerScaling;
    }

    public void setAspectRatio(@NotNull AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return this.aspectRatio;
    }

    public void setVideoSize(@Nullable VideoSize videoSize) {
        this.videoSize = videoSize;
    }

    public Optional<VideoSize> getVideoSize() {
        return Optional.ofNullable(this.videoSize);
    }

}
