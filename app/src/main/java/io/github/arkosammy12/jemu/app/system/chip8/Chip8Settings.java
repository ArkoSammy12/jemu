package io.github.arkosammy12.jemu.app.system.chip8;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.app.system.chip8.database.Chip8Database;
import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Chip8Settings {

    private volatile transient Chip8Database chip8Database;

    private final transient byte[] persistentFlags = new byte[16];

    @SerializedName("setting_source_preference")
    private volatile SettingSourcePreference settingSourcePreference = SettingSourcePreference.PREFER_DATABASE;

    @SerializedName("variant_source")
    private volatile VariantSource variantSource = VariantSource.USE_FROM_DATABASE;

    @SerializedName("color_palette")
    private volatile ColorPaletteSetting colorPaletteSetting = ColorPaletteSetting.UNSPECIFIED;

    @SerializedName("display_orientation")
    private volatile DisplayOrientationSetting displayOrientationSetting = DisplayOrientationSetting.UNSPECIFIED;

    @SerializedName("override_ipf")
    private volatile boolean overrideIpf;

    @SerializedName("ipf")
    private int ipf = 1;

    @SerializedName("do_vf_reset")
    private volatile BooleanQuirkSetting doVFReset = BooleanQuirkSetting.UNSPECIFIED;

    @SerializedName("memory_increment_quirk")
    private volatile MemoryIncrementQuirkSetting memoryIncrementQuirkSetting = MemoryIncrementQuirkSetting.UNSPECIFIED;

    @SerializedName("do_display_wait")
    private volatile BooleanQuirkSetting doDisplayWait = BooleanQuirkSetting.UNSPECIFIED;

    @SerializedName("do_clipping")
    private volatile BooleanQuirkSetting doClipping = BooleanQuirkSetting.UNSPECIFIED;

    @SerializedName("do_shift_vx_in_place")
    private volatile BooleanQuirkSetting doShiftVXInPlace = BooleanQuirkSetting.UNSPECIFIED;

    @SerializedName("do_jump_with_vx")
    private volatile BooleanQuirkSetting doJumpWithVX = BooleanQuirkSetting.UNSPECIFIED;

    Optional<Chip8Database.Entry> getEntryForRom(byte[] rom) {
        if (this.chip8Database == null) {
            this.chip8Database = new Chip8Database();
        }
        if (this.chip8Database == null) {
            return Optional.empty();
        } else {
            return this.chip8Database.getEntryForRom(rom);
        }
    }

    void setPersistentFlag(int index, int value) {
        this.persistentFlags[index] = (byte) value;
    }

    int getPersistentFlag(int index) {
        return (int) this.persistentFlags[index] & 0xFF;
    }

    void setSettingSourcePreference(SettingSourcePreference settingSourcePreference) {
        this.settingSourcePreference = settingSourcePreference;
    }

    public SettingSourcePreference getSettingSourcePreference() {
        return this.settingSourcePreference;
    }

    void setVariantSource(VariantSource variantSource) {
        this.variantSource = variantSource;
    }

    public VariantSource getVariantSource() {
        return this.variantSource;
    }

    void setColorPaletteSetting(ColorPaletteSetting colorPaletteSetting) {
        this.colorPaletteSetting = colorPaletteSetting;
    }

    public Optional<Chip8Host.ColorPalette> getColorPalette() {
        return this.colorPaletteSetting.mapToValue();
    }

    public ColorPaletteSetting getColorPaletteSetting() {
        return this.colorPaletteSetting;
    }

    void setDisplayOrientationSetting(DisplayOrientationSetting displayOrientationSetting) {
        this.displayOrientationSetting = displayOrientationSetting;
    }

    public Optional<VideoGenerator.DisplayOrientation> getDisplayOrientation() {
        return this.displayOrientationSetting.mapToValue();
    }

    public DisplayOrientationSetting getDisplayOrientationSetting() {
        return this.displayOrientationSetting;
    }

    void setOverrideIpf(boolean overrideIpf) {
        this.overrideIpf = overrideIpf;
    }

    public boolean getOverrideIpfSetting() {
        return this.overrideIpf;
    }

    void setIpf(int ipf) {
        this.ipf = ipf;
    }

    public Optional<Integer> getIpf() {
        return this.overrideIpf && this.ipf >= 1 ? Optional.of(this.ipf) : Optional.empty();
    }

    public int getIpfSetting() {
        return this.ipf;
    }

    void setDoVFReset(BooleanQuirkSetting doVFReset) {
        this.doVFReset = doVFReset;
    }

    public Optional<Boolean> getDoVFReset() {
        return this.doVFReset.mapToValue();
    }

    public BooleanQuirkSetting getDoVFResetSetting() {
        return this.doVFReset;
    }

    void setMemoryIncrementQuirkSetting(MemoryIncrementQuirkSetting memoryIncrementQuirkSetting) {
        this.memoryIncrementQuirkSetting = memoryIncrementQuirkSetting;
    }

    public Optional<Chip8Host.MemoryIncrementQuirk> getMemoryIncrementQuirk() {
        return this.memoryIncrementQuirkSetting.mapToHost();
    }

    public MemoryIncrementQuirkSetting getMemoryIncrementQuirkSetting() {
        return this.memoryIncrementQuirkSetting;
    }

    void setDoDisplayWait(BooleanQuirkSetting doDisplayWait) {
        this.doDisplayWait = doDisplayWait;
    }

    public Optional<Boolean> getDoDisplayWait() {
        return this.doDisplayWait.mapToValue();
    }

    public BooleanQuirkSetting getDoDisplayWaitSetting() {
        return this.doDisplayWait;
    }

    void setDoClipping(BooleanQuirkSetting doClipping) {
        this.doClipping = doClipping;
    }

    public Optional<Boolean> getDoClipping() {
        return this.doClipping.mapToValue();
    }

    public BooleanQuirkSetting getDoClippingSetting() {
        return this.doClipping;
    }

    void setDoShiftVXInPlace(BooleanQuirkSetting doShiftVXInPlace) {
        this.doShiftVXInPlace = doShiftVXInPlace;
    }

    public Optional<Boolean> getDoShiftVXInPlace() {
        return this.doShiftVXInPlace.mapToValue();
    }

    public BooleanQuirkSetting getDoShiftVXInPlaceSetting() {
        return this.doShiftVXInPlace;
    }

    void setDoJumpWithVX(BooleanQuirkSetting doJumpWithVX) {
        this.doJumpWithVX = doJumpWithVX;
    }

    public Optional<Boolean> getDoJumpWithVX() {
        return this.doJumpWithVX.mapToValue();
    }

    public BooleanQuirkSetting getDoJumpWithVXSetting() {
        return this.doJumpWithVX;
    }

    public enum SettingSourcePreference implements DisplayNamerProvider {
        @SerializedName("prefer_overrides")
        PREFER_OVERRIDES("Prefer overrides"),

        @SerializedName("prefer_database")
        PREFER_DATABASE("Prefer database"),

        @SerializedName("prefer_variant")
        PREFER_VARIANT("Prefer variant")
        ;

        private final String displayName;

        SettingSourcePreference(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

    }

    public enum VariantSource implements DisplayNamerProvider {
        @SerializedName("use_selected")
        USE_SELECTED("Use selected"),

        @SerializedName("use_from_database")
        USE_FROM_DATABASE("Use from database")
        ;

        private final String displayName;

        VariantSource(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

    }

    public enum ColorPaletteSetting implements DisplayNamerProvider {
        @SerializedName("unspecified")
        UNSPECIFIED(null),

        @SerializedName("cadmium")
        CADMIUM(BuiltInChip8Palette.CADMIUM),

        @SerializedName("silicon8")
        SILICON_8(BuiltInChip8Palette.SILICON8),

        @SerializedName("pico8")
        PICO_8(BuiltInChip8Palette.PICO8),

        @SerializedName("octoclassic")
        OCTO_CLASSIC(BuiltInChip8Palette.OCTO_CLASSIC),

        @SerializedName("lcd")
        LCD(BuiltInChip8Palette.LCD),

        @SerializedName("c64")
        C64(BuiltInChip8Palette.C64),

        @SerializedName("intellivision")
        INTELLIVISION(BuiltInChip8Palette.INTELLIVISION),

        @SerializedName("cga")
        CGA(BuiltInChip8Palette.CGA)
        ;

        @Nullable
        private final BuiltInChip8Palette builtInChip8Palette;

        ColorPaletteSetting(@Nullable BuiltInChip8Palette builtInChip8Palette) {
            this.builtInChip8Palette = builtInChip8Palette;
        }

        @Override
        public String getDisplayName() {
            return this.builtInChip8Palette == null ? "Unspecified" : this.builtInChip8Palette.getDisplayName();
        }

        public Optional<Chip8Host.ColorPalette> mapToValue() {
            return Optional.ofNullable(this.builtInChip8Palette);
        }

    }

    public enum DisplayOrientationSetting implements DisplayNamerProvider {
        @SerializedName("unspecified")
        UNSPECIFIED("Unspecified", null),

        @SerializedName("deg_0")
        DEG_0("0 degrees", VideoGenerator.DisplayOrientation.DEG_0),

        @SerializedName("deg_90")
        DEG_90("90 degrees", VideoGenerator.DisplayOrientation.DEG_90),

        @SerializedName("deg_180")
        DEG_180("180 degrees", VideoGenerator.DisplayOrientation.DEG_180),

        @SerializedName("deg_270")
        DEG_270("270 degrees", VideoGenerator.DisplayOrientation.DEG_270)
        ;

        private final String displayName;

        @Nullable
        private final VideoGenerator.DisplayOrientation displayOrientation;

        DisplayOrientationSetting(String displayName, @Nullable VideoGenerator.DisplayOrientation displayOrientation) {
            this.displayName = displayName;
            this.displayOrientation = displayOrientation;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public Optional<VideoGenerator.DisplayOrientation> mapToValue() {
            return Optional.ofNullable(this.displayOrientation);
        }

    }

    public enum BooleanQuirkSetting implements DisplayNamerProvider {
        @SerializedName("unspecified")
        UNSPECIFIED("Unspecified"),

        @SerializedName("enabled")
        ENABLED("Enabled"),

        @SerializedName("disabled")
        DISABLED("Disabled")
        ;

        private final String displayName;

        BooleanQuirkSetting(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public Optional<Boolean> mapToValue() {
            return switch (this) {
                case UNSPECIFIED -> Optional.empty();
                case ENABLED -> Optional.of(true);
                case DISABLED -> Optional.of(false);
            };
        }

    }

    public enum MemoryIncrementQuirkSetting implements DisplayNamerProvider {
        @SerializedName("unspecified")
        UNSPECIFIED("Unspecified", null),

        @SerializedName("none")
        NO_INCREMENT("No increment", Chip8Host.MemoryIncrementQuirk.NO_INCREMENT),

        @SerializedName("increment_by_x")
        INCREMENT_BY_X("Increment by X", Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X),

        @SerializedName("increment_by_x_plus_one")
        INCREMENT_BY_X_1("Increment by X + 1", Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1)
        ;

        private final String displayName;

        @Nullable
        private final Chip8Host.MemoryIncrementQuirk memoryIncrementQuirk;

        MemoryIncrementQuirkSetting(String displayName, @Nullable Chip8Host.MemoryIncrementQuirk memoryIncrementQuirk) {
            this.displayName = displayName;
            this.memoryIncrementQuirk = memoryIncrementQuirk;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public Optional<Chip8Host.MemoryIncrementQuirk> mapToHost() {
            return Optional.ofNullable(this.memoryIncrementQuirk);
        }

    }

    enum BooleanQuirks {
        VF_RESET,
        DISPLAY_WAIT,
        CLIPPING,
        SHIFT_VX_IN_PLACE,
        JUMP_WITH_VX
    }

}
