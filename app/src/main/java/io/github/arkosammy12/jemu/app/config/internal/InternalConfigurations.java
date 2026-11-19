package io.github.arkosammy12.jemu.app.config.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.config.Configurations;
import io.github.arkosammy12.jemu.app.config.settings.PreferenceSettings;
import io.github.arkosammy12.jemu.app.config.settings.internal.InternalPreferenceSettings;
import io.github.arkosammy12.jemu.app.config.state.State;

public class InternalConfigurations implements Configurations {

    @SerializedName("settings")
    private final InternalPreferenceSettings preferenceSettings = new InternalPreferenceSettings();

    @SerializedName("state")
    private final State state = new State();

    @Override
    public PreferenceSettings getSettings() {
        return this.preferenceSettings;
    }

    public InternalPreferenceSettings getInternalPreferenceSettings() {
        return this.preferenceSettings;
    }

    public State getState() {
        return this.state;
    }

}
