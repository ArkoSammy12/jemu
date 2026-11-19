package io.github.arkosammy12.jemu.app.config;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.config.settings.Settings;
import io.github.arkosammy12.jemu.app.config.state.State;

public class Configurations {
    
    @SerializedName("settings")
    private final Settings preferenceSettings = new Settings();

    @SerializedName("state")
    private final State state = new State();

    public Settings getSettings() {
        return this.preferenceSettings;
    }

    public Settings getPreferenceSettings() {
        return this.preferenceSettings;
    }

    public State getState() {
        return this.state;
    }
}
