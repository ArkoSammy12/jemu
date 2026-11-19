package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.util.DisplayNamerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class VideoSettings {

    public enum AspectRatio implements DisplayNamerProvider {
        @SerializedName("auto")
        AUTO(1.0, "Auto"),

        @SerializedName("pixel_perfect")
        PIXEL_PERFECT(1.0, "Pixel Perfect (1:1)"),

        @SerializedName("ntsc")
        NTSC(8.0 / 7.0, "NTSC (8:7)"),

        @SerializedName("pal")
        PAL(18.0 / 13.0, "PAL (18:13)"),

        @SerializedName("standard")
        STANDARD(4.0 / 3.0, "Standard (4:3)"),

        @SerializedName("widescreen")
        WIDESCREEN(16.0 / 9.0, "Widescreen (16:9)");

        private final double pixelAspectRatio;
        private final String displayName;

        AspectRatio(double pixelAspectRatio, String displayName) {
            this.pixelAspectRatio = pixelAspectRatio;
            this.displayName = displayName;
        }

        public double getPixelAspectRatio() {
            return this.pixelAspectRatio;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

    }
    
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

    public boolean getUseIntegerScaling() {
        return this.useIntegerScaling;
    }

    public void setAspectRatio(@NotNull AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

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
