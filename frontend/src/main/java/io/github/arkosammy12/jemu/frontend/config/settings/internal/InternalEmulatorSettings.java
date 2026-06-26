package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class InternalEmulatorSettings {

    @Nullable
    @SerializedName("selected_system_id")
    private String selectedSystemId = null;

    public void setSelectedSystemId(@Nullable String systemId) {
        this.selectedSystemId = systemId;
    }

    public Optional<String> getSelectedSystemId() {
        return Optional.ofNullable(this.selectedSystemId);
    }

}
