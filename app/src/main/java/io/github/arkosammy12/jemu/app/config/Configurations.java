package io.github.arkosammy12.jemu.app.config;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.config.settings.PreferenceSettings;
import io.github.arkosammy12.jemu.app.config.state.State;

public class Configurations {
    
    @SerializedName("settings")
    private final PreferenceSettings preferenceSettings = new PreferenceSettings();

    @SerializedName("state")
    private final State state = new State();

    public PreferenceSettings getSettings() {
        return this.preferenceSettings;
    }

    public PreferenceSettings getPreferenceSettings() {
        return this.preferenceSettings;
    }

    public State getState() {
        return this.state;
    }
}
