package io.github.arkosammy12.jemu.app.system.commodore64;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.NotNull;
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

    @SerializedName("vicii-palette")
    private volatile VICIIPalette viciiPalette = VICIIPalette.PC64;

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

    void setVICIIPalette(@NotNull VICIIPalette viciiPalette) {
        this.viciiPalette = viciiPalette;
    }

    public VICIIPalette getVICIIPalette() {
        return this.viciiPalette;
    }

    public enum VICIIPalette implements DisplayNamerProvider {

        // https://www.godot64.de/german/hpalet.htm
        @SerializedName("godots")
        GODOTS("Godots", new int[] {
                0x000000, 0xFFFFFF, 0x880000, 0xAAFFEE,
                0xCCFFCC, 0x00CC55, 0x0000AA, 0xEEEE77,
                0xDD8855, 0x664400, 0xFF7777, 0x333333,
                0x777777, 0xAAFF66, 0x0088FF, 0xBBBBBB
        }),

        @SerializedName("gulrak")
        GULRAK("Gulrak", new int[] {
                0x000000, 0xFFFFFF, 0x753B2F, 0x73AEBE,
                0x784193, 0x619A47, 0x392C85, 0xC2D073,
                0x7B5629, 0x4D4000, 0xA76B5D, 0x4A4A4A,
                0x707070, 0xA1D988, 0x7062C0, 0x989898
        }),

        // http://www.pepto.de/projects/colorvic/
        @SerializedName("colodore")
        COLODORE("Colodore", new int[] {
                0x000000, 0xFFFFFF, 0x68372B, 0x70A4B2,
                0x6F3D86, 0x588D43, 0x352879, 0xB8C76F,
                0x6F4F25, 0x433900, 0x9A6759, 0x444444,
                0x6C6C6C, 0x9AD284, 0x6C5EB5, 0x959595
        }),

        @SerializedName("pc64")
        PC64("PC64", new int[] {
                0x202020, 0xFFFFFF, 0xB62020, 0x71FFFF,
                0xB620B6, 0x20B620, 0x2020B6, 0xFFFF20,
                0xB67120, 0x914420, 0xFF7171, 0x717171,
                0x919191, 0x71FF71, 0x7171FF, 0xB6B6B6
        })
        ;

        private final String displayName;
        private final int[] rgb8;

        VICIIPalette(String displayName, int[] rgb8) {
            this.displayName = displayName;
            this.rgb8 = rgb8;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public int getRGB8ForPaletteIndex(int paletteIndex) {
            return this.rgb8[paletteIndex];
        }

    }

}
