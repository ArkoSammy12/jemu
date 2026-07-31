package io.github.arkosammy12.jemu.frontend.config.settings;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

public interface VideoSettings {

    boolean getUseIntegerScaling();

    AspectRatio getAspectRatio();

    enum AspectRatio implements DisplayNamerProvider {
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

}
