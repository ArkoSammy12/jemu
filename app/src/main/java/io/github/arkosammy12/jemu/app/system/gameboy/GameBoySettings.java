package io.github.arkosammy12.jemu.app.system.gameboy;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.core.gameboy.DMGPPU;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class GameBoySettings {

    @SerializedName("prefer_gameboy_color")
    private volatile boolean preferGameBoyColor = false;

    @SerializedName("palette")
    private volatile DMGPalette dmgPalette = DMGPalette.DMG_GREEN;

    @SerializedName("use_builtin_bootrom")
    private volatile boolean useBuiltInBootROM = true;

    @Nullable
    @SerializedName("gameboy_bootrom_path")
    private volatile String gameBoyBootRomPath;

    @Nullable
    @SerializedName("gameboy_color_bootrom_path")
    private volatile String gameBoyColorBootRomPath;

    void setPreferGameBoyColor(boolean preferGameBoyColor) {
        this.preferGameBoyColor = preferGameBoyColor;
    }

    public boolean preferGameBoyColor() {
        return this.preferGameBoyColor;
    }

    void setDMGPalette(DMGPalette dmgPalette) {
        this.dmgPalette = dmgPalette;
    }

    public DMGPalette getDMGPalette() {
        return this.dmgPalette;
    }

    void setUseBuiltInBootROM(boolean useBuiltInBootROM) {
        this.useBuiltInBootROM = useBuiltInBootROM;
    }

    public boolean useBuiltInBootROM() {
        return this.useBuiltInBootROM;
    }

    void setGameBoyBootRomPath(@Nullable Path path) {
        this.gameBoyBootRomPath = path == null ? null : path.toString();
    }

    public Optional<Path> getGameBoyBootROMPath() {
        return Optional.ofNullable(this.gameBoyBootRomPath).map(Paths::get);
    }

    void setGameBoyColorBootROMPath(@Nullable Path path) {
        this.gameBoyColorBootRomPath = path == null ? null : path.toString();
    }

    public Optional<Path> getGameBoyColorBootRomPath() {
        return Optional.ofNullable(this.gameBoyColorBootRomPath).map(Paths::get);
    }

    public enum DMGPalette implements DisplayNamerProvider {
        @SerializedName("gameboy_green")
        DMG_GREEN("Game Boy Green", DMGPPU.Palette.DMG_GREEN),

        @SerializedName("greyscale")
        GREYSCALE("Greyscale", DMGPPU.Palette.GREYSCALE),

        @SerializedName("sameboy")
        SAMEBOY("SameBoy", DMGPPU.Palette.SAMEBOY)
        ;

        private final String displayName;
        private final DMGPPU.Palette dmgPalette;

        DMGPalette(String displayName, DMGPPU.Palette dmgPalette) {
            this.displayName = displayName;
            this.dmgPalette = dmgPalette;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public DMGPPU.Palette mapToHost() {
            return this.dmgPalette;
        }

    }

}
