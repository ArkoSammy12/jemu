package io.github.arkosammy12.jemu.frontend.audio;

import com.google.gson.annotations.SerializedName;

public enum SampleRate {

    @SerializedName("96000_hz")
    HZ_96000(96000, "96000 Hz"),

    @SerializedName("48000_hz")
    HZ_48000(48000, "48000 Hz"),

    @SerializedName("44100_hz")
    HZ_44100(44100, "44100 Hz"),

    @SerializedName("22050_hz")
    HZ_22050(22050, "22050 Hz"),

    @SerializedName("8000_hz")
    HZ_8000(8000, "8000 Hz");


    private final int intValue;
    private final String displayName;

    SampleRate(int intValue, String displayName) {
        this.intValue = intValue;
        this.displayName = displayName;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public String getDisplayName() {
        return this.displayName;
    }

}
