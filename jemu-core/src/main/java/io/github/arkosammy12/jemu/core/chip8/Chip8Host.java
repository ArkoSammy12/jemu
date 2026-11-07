package io.github.arkosammy12.jemu.core.chip8;

import io.github.arkosammy12.jemu.core.common.SystemHost;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;

import java.util.Optional;

public interface Chip8Host extends SystemHost {

    ColorPalette getColorPalette();

    VideoGenerator.DisplayOrientation getDisplayOrientation();

    SpriteFont getSpriteFont();

    boolean doVFReset();

    MemoryIncrementQuirk getMemoryIncrementQuirk();

    boolean doDisplayWait();

    boolean doClipping();

    boolean doShiftVXInPlace();

    boolean doJumpWithVX();

    void setPersistentFlag(int index, int value);

    int getPersistentFlag(int index);

    void exit();

    interface SpriteFont {

        Optional<byte[][]> getSmallFont();

        Optional<byte[][]> getBigFont();

        int getSmallFontBeginOffset();

        int getBigFontBeginOffset();

        int getSmallFontSpriteOffset(int hex);

        int getBigFontSpriteOffset(int hex);

    }

    interface ColorPalette {

        int getRGB8ForIndex(int colorIndex);

    }

    enum MemoryIncrementQuirk {
        NO_INCREMENT,
        INCREMENT_BY_X,
        INCREMENT_BY_X_PLUS_1
    }

}