package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;

public class WindowSettings {

    @SerializedName("always_on_top")
    private volatile boolean alwaysOnTop = false;

    @SerializedName("start_in_fullscreen")
    private volatile boolean startInFullscreen = false;

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    public boolean getAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    public void setStartInFullscreen(boolean startInFullscreen) {
        this.startInFullscreen = startInFullscreen;
    }

    public boolean getStartInFullscreen() {
        return this.startInFullscreen;
    }

}
