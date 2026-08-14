package io.github.arkosammy12.jemu.app.system.commodore64;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Commodore64Settings {

    @Nullable
    @SerializedName("kernal_rom_path")
    private volatile String kernalRomPath;

    @Nullable
    @SerializedName("basic_rom_path")
    private volatile String basicRomPath;

    @Nullable
    @SerializedName("character_rom_path")
    private volatile String characterRomPath;

    void setKernalRomPath(@Nullable Path path) {
        this.kernalRomPath = path == null ? null : path.toString();
    }

    public Optional<Path> getKernalRomPath() {
        return Optional.ofNullable(this.kernalRomPath).map(Paths::get);
    }

    void setBasicRomPath(@Nullable Path path) {
        this.basicRomPath = path == null ? null : path.toString();
    }

    public Optional<Path> getBasicRomPath() {
        return Optional.ofNullable(this.basicRomPath).map(Paths::get);
    }

    void setCharacterRomPath(@Nullable Path path) {
        this.characterRomPath = path == null ? null : path.toString();
    }

    public Optional<Path> getCharacterRomPath() {
        return Optional.ofNullable(this.characterRomPath).map(Paths::get);
    }

}
