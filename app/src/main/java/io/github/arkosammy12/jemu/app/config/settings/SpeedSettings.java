package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class SpeedSettings {

    @SerializedName("speed_mode")
    private volatile SpeedMode speedMode = SpeedMode.NORMAL;

    public void setSpeedMode(@NotNull SpeedMode speedMode) {
        this.speedMode = speedMode;
    }

    public SpeedMode getSpeedMode() {
        return this.speedMode;
    }

}
