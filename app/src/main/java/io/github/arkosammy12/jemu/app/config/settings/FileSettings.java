package io.github.arkosammy12.jemu.app.config.settings;

import com.google.gson.annotations.SerializedName;

public class FileSettings {
    
    @SerializedName("reset_on_rom_file_select")
    private volatile boolean resetOnRomFileSelect = true;

    public void setResetOnRomFileSelect(boolean resetOnRomFileSelect) {
        this.resetOnRomFileSelect = resetOnRomFileSelect;
    }

    public boolean getResetOnROMFileSelect() {
        return this.resetOnRomFileSelect;
    }

}
