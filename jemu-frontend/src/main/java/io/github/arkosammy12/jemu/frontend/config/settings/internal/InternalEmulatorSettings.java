package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class InternalEmulatorSettings {

    @Nullable
    @SerializedName("selected_system_id")
    private String selectedSystemId = null;

    @Nullable
    @SerializedName("emulation")
    private volatile JsonElement emulationSettings;

    public void setSelectedSystemId(@Nullable String systemId) {
        this.selectedSystemId = systemId;
    }

    public Optional<String> getSelectedSystemId() {
        return Optional.ofNullable(this.selectedSystemId);
    }

    public void setEmulationSettings(@Nullable JsonElement emulationSettings) {
        this.emulationSettings = emulationSettings;
    }

    public Optional<JsonElement> getEmulationSettings() {
        return Optional.ofNullable(this.emulationSettings);
    }

}
