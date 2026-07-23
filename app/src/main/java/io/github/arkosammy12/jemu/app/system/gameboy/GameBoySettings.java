package io.github.arkosammy12.jemu.app.system.gameboy;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.core.gameboy.DMGPPU;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

public class GameBoySettings {

    @SerializedName("palette")
    private volatile DMGPalette dmgPalette = DMGPalette.DMG_GREEN;

    void setDMGPalette(DMGPalette dmgPalette) {
        this.dmgPalette = dmgPalette;
    }

    public DMGPalette getDMGPalette() {
        return this.dmgPalette;
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
