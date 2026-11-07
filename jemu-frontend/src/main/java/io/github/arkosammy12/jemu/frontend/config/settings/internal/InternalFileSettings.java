package io.github.arkosammy12.jemu.frontend.config.settings.internal;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.config.settings.FileSettings;

public class InternalFileSettings implements FileSettings {

    @SerializedName("reset_on_rom_file_select")
    private volatile boolean resetOnRomFileSelect = true;

    public void setResetOnRomFileSelect(boolean resetOnRomFileSelect) {
        this.resetOnRomFileSelect = resetOnRomFileSelect;
    }

    @Override
    public boolean getResetOnROMFileSelect() {
        return this.resetOnRomFileSelect;
    }


}
