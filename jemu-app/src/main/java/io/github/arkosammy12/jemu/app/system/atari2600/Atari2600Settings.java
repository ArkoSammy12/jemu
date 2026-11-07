package io.github.arkosammy12.jemu.app.system.atari2600;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Cartridge;
import io.github.arkosammy12.jemu.core.atari2600.Atari2600Emulator;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Atari2600Settings {

    @SerializedName("tv_type")
    private volatile TVType tvType = TVType.COLOR;

    @SerializedName("left_difficulty")
    private volatile PlayerDifficulty leftDifficulty = PlayerDifficulty.BEGINNER;

    @SerializedName("right_difficulty")
    private volatile PlayerDifficulty rightDifficulty = PlayerDifficulty.BEGINNER;

    @SerializedName("tv_format")
    private volatile TVFormatOverride tvFormatOverride = TVFormatOverride.NONE;

    @SerializedName("cartridge_type")
    private volatile CartridgeTypeOverride cartridgeTypeOverride = CartridgeTypeOverride.NONE;

    void setTVType(TVType tvType) {
        this.tvType = tvType;
    }

    public TVType getTVType() {
        return this.tvType;
    }

    void setLeftDifficulty(PlayerDifficulty playerDifficulty) {
        this.leftDifficulty = playerDifficulty;
    }

    public PlayerDifficulty getLeftDifficulty() {
        return this.leftDifficulty;
    }

    void setRightDifficulty(PlayerDifficulty playerDifficulty) {
        this.rightDifficulty = playerDifficulty;
    }

    public PlayerDifficulty getRightDifficulty() {
        return this.rightDifficulty;
    }

    void setTVFormatOverride(TVFormatOverride tvFormatOverride) {
        this.tvFormatOverride = tvFormatOverride;
    }

    public TVFormatOverride getTVFormatOverride() {
        return this.tvFormatOverride;
    }

    void setCartridgeTypeOverride(CartridgeTypeOverride cartridgeTypeOverride) {
        this.cartridgeTypeOverride = cartridgeTypeOverride;
    }

    public CartridgeTypeOverride getCartridgeTypeOverride() {
        return this.cartridgeTypeOverride;
    }

    public enum PlayerDifficulty implements DisplayNamerProvider {
        @SerializedName("advanced")
        ADVANCED("Advanced"),

        @SerializedName("beginner")
        BEGINNER("Beginner");

        private final String displayName;

        PlayerDifficulty(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

    }

    public enum TVType implements DisplayNamerProvider {
        @SerializedName("color")
        COLOR("Color"),

        @SerializedName("black_and_white")
        BLACK_AND_WHITE("Black and White");

        private final String displayName;

        TVType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

    }

    public enum TVFormatOverride implements DisplayNamerProvider {
        @SerializedName("none")
        NONE("None", null),

        @SerializedName("ntsc")
        NTSC("NTSC", Atari2600Emulator.TVFormat.NTSC),

        @SerializedName("pal")
        PAL("PAL", Atari2600Emulator.TVFormat.PAL),

        @SerializedName("secam")
        SECAM("SECAM", Atari2600Emulator.TVFormat.SECAM),

        @SerializedName("ntsc50")
        NTSC50("NTSC50", Atari2600Emulator.TVFormat.NTSC50),

        @SerializedName("pal60")
        PAL60("PAL60", Atari2600Emulator.TVFormat.PAL60),

        @SerializedName("secam60")
        SECAM60("SECAM60", Atari2600Emulator.TVFormat.SECAM60);

        private final String displayName;

        @Nullable
        private final Atari2600Emulator.TVFormat tvFormat;

        TVFormatOverride(String displayName, Atari2600Emulator.@Nullable TVFormat tvFormat) {
            this.displayName = displayName;
            this.tvFormat = tvFormat;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public Optional<Atari2600Emulator.TVFormat> getHostTVFormat() {
            return Optional.ofNullable(this.tvFormat);
        }

    }

    public enum CartridgeTypeOverride implements DisplayNamerProvider {
        @SerializedName("none")
        NONE("None", null),

        @SerializedName("2k")
        CART_2K("2K", Atari2600Cartridge.Type.CART_2K),

        @SerializedName("4k")
        CART_4K("4K", Atari2600Cartridge.Type.CART_4K),

        //@SerializedName("4ksc")
        //CART_4KSC("4KSC", Atari2600Cartridge.Type.CART_4KSC),

        @SerializedName("f4")
        CART_F4("F4", Atari2600Cartridge.Type.CART_F4),

        //@SerializedName("f4sc")
        //CART_F4SC("F4SC", Atari2600Cartridge.Type.CART_F4SC),

        @SerializedName("f6")
        CART_F6("F6", Atari2600Cartridge.Type.CART_F6),

        //@SerializedName("f6sc")
        //CART_F6SC("F6SC", Atari2600Cartridge.Type.CART_F6SC),

        @SerializedName("f8")
        CART_F8("F8", Atari2600Cartridge.Type.CART_F8),

        //@SerializedName("f8sc")
        //CART_F8SC("F8SC", Atari2600Cartridge.Type.CART_F8SC),

        //@SerializedName("f0")
        //CART_F0("F0", Atari2600Cartridge.Type.CART_F0),

        @SerializedName("fa")
        CART_FA("FA", Atari2600Cartridge.Type.CART_FA),

        //@SerializedName("fa2")
        //CART_FA2("FA2", Atari2600Cartridge.Type.CART_FA2),

        @SerializedName("fe")
        CART_FE("FE", Atari2600Cartridge.Type.CART_FE),

        //@SerializedName("e0")
        //CART_E0("E0", Atari2600Cartridge.Type.CART_E0),

        //@SerializedName("e7")
        //CART_E7("E7", Atari2600Cartridge.Type.CART_E7),

        //@SerializedName("ef")
        //CART_EF("EF", Atari2600Cartridge.Type.CART_EF),

        //@SerializedName("efsc")
        //CART_EFSC("EFSC", Atari2600Cartridge.Type.CART_EFSC),

        @SerializedName("3e")
        CART_3E("3E", Atari2600Cartridge.Type.CART_3E),

        //@SerializedName("3e+")
        //CART_3EPLUS("3E+", Atari2600Cartridge.Type.CART_3EPLUS),

        @SerializedName("3f")
        CART_3F("3F", Atari2600Cartridge.Type.CART_3F),

        @SerializedName("0840")
        CART_0840("0840", Atari2600Cartridge.Type.CART_0840),

        //@SerializedName("4a50")
        //CART_4A50("4A50", Atari2600Cartridge.Type.CART_4A50),

        //@SerializedName("ar")
        //CART_AR("AR", Atari2600Cartridge.Type.CART_AR),

        //@SerializedName("cv")
        //CART_CV("CV", Atari2600Cartridge.Type.CART_CV),

        //@SerializedName("ua")
        //CART_UA("UA", Atari2600Cartridge.Type.CART_UA),

        //@SerializedName("sb")
        //CART_SB("SB", Atari2600Cartridge.Type.CART_SB),

        //@SerializedName("wd")
        //CART_WD("WD", Atari2600Cartridge.Type.CART_WD),

        //@SerializedName("x07")
        //CART_X07("X07", Atari2600Cartridge.Type.CART_X07),

        //@SerializedName("mdm")
        //CART_MDM("MDM", Atari2600Cartridge.Type.CART_MDM),

        //@SerializedName("mvc")
        //CART_MVC("MVC", Atari2600Cartridge.Type.CART_MVC),

        //@SerializedName("bf")
        //CART_BF("BF", Atari2600Cartridge.Type.CART_BF),

        //@SerializedName("bfsc")
        //CART_BFSC("BFSC", Atari2600Cartridge.Type.CART_BFSC),

        //@SerializedName("df")
        //CART_DF("DF", Atari2600Cartridge.Type.CART_DF),

        //@SerializedName("dfsc")
        //CART_DFSC("DFSC", Atari2600Cartridge.Type.CART_DFSC),

        //@SerializedName("dpc")
        //CART_DPC("DPC", Atari2600Cartridge.Type.CART_DPC),

        //@SerializedName("dpc+")
        //CART_DPCPLUS("DPC+", Atari2600Cartridge.Type.CART_DPCPLUS),

        //@SerializedName("cdf")
        //CART_CDF("CDF", Atari2600Cartridge.Type.CART_CDF),

        //@SerializedName("gl")
        //CART_GL("GL", Atari2600Cartridge.Type.CART_GL),

        //@SerializedName("tvboy")
        //CART_TVBOY("TVBoy", Atari2600Cartridge.Type.CART_TVBOY),

        ;

        private final String displayName;

        @Nullable
        private final Atari2600Cartridge.Type cartridgeType;

        CartridgeTypeOverride(String displayName, Atari2600Cartridge.@Nullable Type cartridgeType) {
            this.displayName = displayName;
            this.cartridgeType = cartridgeType;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public Optional<Atari2600Cartridge.Type> getHostCartridgeType() {
            return Optional.ofNullable(this.cartridgeType);
        }

    }

}
