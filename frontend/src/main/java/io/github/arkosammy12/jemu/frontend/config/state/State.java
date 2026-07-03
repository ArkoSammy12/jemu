package io.github.arkosammy12.jemu.frontend.config.state;

import com.google.gson.annotations.SerializedName;

public class State {

    @SerializedName("file")
    private final FileState fileState = new FileState();

    @SerializedName("window")
    private final WindowState windowState = new WindowState();

    public FileState getFileState() {
        return this.fileState;
    }

    public WindowState getWindowState() {
        return this.windowState;
    }

}
