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
        DMG_GREEN("Game Boy Green", new int[] {
                0x9BBC0F,
                0x8BAC0F,
                0x306230,
                0x0F380F,
                0x9BBC0F // LCD off color
        }),

        @SerializedName("greyscale")
        GREYSCALE("Greyscale", new int[] {
                0xFFFFFF,
                0xC0C0C0,
                0x404040,
                0x000000,
                0xFFFFFF // LCD off color
        }),

        @SerializedName("sameboy")
        SAMEBOY("SameBoy", new int[] {
                0xC6DE8C,
                0x84A563,
                0x396139,
                0x081810,
                0xD2E6A6 // LCD off color
        })
        ;

        private final String displayName;
        private final int[] rgb;

        DMGPalette(String displayName, int[] rgb8) {
            this.displayName = displayName;
            this.rgb = rgb8;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public int getRGB8ForDMGPaletteIndex(int paletteIndex) {
            return this.rgb[paletteIndex];
        }

    }

}
