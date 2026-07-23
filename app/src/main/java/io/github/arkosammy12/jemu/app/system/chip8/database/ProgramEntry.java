package io.github.arkosammy12.jemu.app.system.chip8.database;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Optional;

class ProgramEntry {

    @SerializedName(value = "title")
    private String title;

    @SerializedName(value = "roms")
    private Map<String, RomEntry> roms;

    Optional<String> getTitle() {
        return Optional.ofNullable(this.title);
    }

    Optional<Map<String, RomEntry>> getRomEntries() {
        return Optional.ofNullable(this.roms);
    }

}
