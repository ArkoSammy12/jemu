package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.config.settings.SpeedSettings;
import org.jetbrains.annotations.NotNull;

public class InternalSpeedSettings implements SpeedSettings {

    @SerializedName("speed_mode")
    private volatile SpeedMode speedMode = SpeedMode.NORMAL;

    public void setSpeedMode(@NotNull SpeedMode speedMode) {
        this.speedMode = speedMode;
    }

    @Override
    public SpeedMode getSpeedMode() {
        return this.speedMode;
    }
}
