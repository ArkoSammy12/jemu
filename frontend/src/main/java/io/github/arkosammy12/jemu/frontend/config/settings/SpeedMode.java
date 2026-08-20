package io.github.arkosammy12.jemu.frontend.config.settings;

import com.google.gson.annotations.SerializedName;

public enum SpeedMode {
    @SerializedName("normal")
    NORMAL("Normal (100%)"),

    @SerializedName("unlimited")
    UNLIMITED("Unlimited"),

    @SerializedName("triple")
    TRIPLE("Triple (300%)"),

    @SerializedName("double")
    DOUBLE("Double (200%)"),

    @SerializedName("half")
    HALF("Half (50%)"),

    @SerializedName("quarter")
    QUARTER("Quarter (25%)");

    private final String displayName;

    SpeedMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int scaleFramerate(int framerate) {
        return Math.clamp(switch (this) {
            case NORMAL -> framerate;
            case UNLIMITED -> -1;
            case TRIPLE -> framerate * 3L;
            case DOUBLE -> framerate * 2L;
            case HALF -> Math.round(framerate * 0.50);
            case QUARTER -> Math.round(framerate * 0.25);
        }, 1, 300);
    }

}
