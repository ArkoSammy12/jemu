package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.config.settings.WindowSettings;

public class InternalWindowSettings implements WindowSettings {

    @SerializedName("always_on_top")
    private volatile boolean alwaysOnTop = false;

    @SerializedName("start_in_fullscreen")
    private volatile boolean startInFullscreen = false;

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    @Override
    public boolean getAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    public void setStartInFullscreen(boolean startInFullscreen) {
        this.startInFullscreen = startInFullscreen;
    }

    @Override
    public boolean getStartInFullscreen() {
        return this.startInFullscreen;
    }

}
