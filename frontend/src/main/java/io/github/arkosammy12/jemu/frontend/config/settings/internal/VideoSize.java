package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;

public enum VideoSize {
    @SerializedName("1x")
    SCALE_1X(1, "1×"),

    @SerializedName("2x")
    SCALE_2X(2, "2×"),

    @SerializedName("3x")
    SCALE_3X(3, "3×"),

    @SerializedName("4x")
    SCALE_4X(4, "4×"),

    @SerializedName("5x")
    SCALE_5X(5, "5×");

    private final int scaleFactor;
    private final String displayName;

    VideoSize(int scaleFactor, String displayName) {
        this.scaleFactor = scaleFactor;
        this.displayName = displayName;
    }

    public int getScaleFactor() {
        return this.scaleFactor;
    }

    public String getDisplayName() {
        return this.displayName;
    }

}
