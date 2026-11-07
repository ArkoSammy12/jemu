package io.github.arkosammy12.jemu.app.system.chip8.database;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

class PlatformEntry {

    @SerializedName(value = "id")
    private String id;

    @SerializedName(value = "quirks")
    private Quirks quirks;

    Optional<String> getId() {
        return Optional.ofNullable(this.id);
    }

    Optional<Quirks> getQuirks() {
        return Optional.ofNullable(this.quirks);
    }

}

