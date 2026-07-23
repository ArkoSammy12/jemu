package io.github.arkosammy12.jemu.app.system.chip8.database;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class RomEntry {

    @SerializedName(value = "platforms")
    private List<String> platforms;

    @SerializedName(value = "quirkyPlatforms")
    private Map<String, Quirks> quirkyPlatforms;

    @SerializedName(value = "tickrate")
    private Integer tickRate;

    @SerializedName(value = "screenRotation")
    private Integer screenRotation;

    @SerializedName(value = "colors")
    private Colors colors;

    Optional<List<String>> getPlatforms() {
        return Optional.ofNullable(this.platforms);
    }

    Optional<Map<String, Quirks>> getQuirkyPlatforms() {
        return Optional.ofNullable(this.quirkyPlatforms);
    }

    Optional<Integer> getTickRate() {
        return Optional.ofNullable(this.tickRate);
    }

    Optional<Integer> getScreenRotation() {
        return Optional.ofNullable(this.screenRotation);
    }

    Optional<Colors> getColors() {
        return Optional.ofNullable(this.colors);
    }

    static class Colors {

        @SerializedName(value = "pixels")
        List<String> pixels;

        Optional<List<String>> getPixels() {
            return Optional.ofNullable(this.pixels);
        }

    }

}
