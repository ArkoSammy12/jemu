package io.github.arkosammy12.jemu.frontend.audio;

import com.google.gson.annotations.SerializedName;

public enum SampleRate {
    @SerializedName("44100_hz")
    HZ_44100("44100 Hz"),

    @SerializedName("48000_hz")
    HZ_48000("48000 Hz");

    private final String displayName;

    SampleRate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

}
