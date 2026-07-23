package io.github.arkosammy12.jemu.app.system.chip8;

import com.google.gson.annotations.SerializedName;
import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

public enum BuiltInChip8Palette implements Chip8Host.ColorPalette, DisplayNamerProvider {
    CADMIUM("Cadmium", new int[] {
            0x1a1c2c, 0xf4f4f4, 0x94b0c2, 0x333c57,
            0xb13e53, 0xa7f070, 0x3b5dc9, 0xffcd75,
            0x5d275d, 0x38b764, 0x29366f, 0x566c86,
            0xef7d57, 0x73eff7, 0x41a6f6, 0x257179
    }),
    SILICON8("Silicon8", new int[] {
            0x000000, 0xffffff, 0xaaaaaa, 0x555555,
            0xff0000, 0x00ff00, 0x0000ff, 0xffff00,
            0x880000, 0x008800, 0x000088, 0x888800,
            0xff00ff, 0x00ffff, 0x880088, 0x008888
    }),
    PICO8("Pico8", new int[] {
            0x000000, 0xfff1e8, 0xc2c3c7, 0x5f574f,
            0xef7d57, 0x00e436, 0x29adff, 0xffec27,
            0xab5236, 0x008751, 0x1d2b53, 0xffa300,
            0xff77a8, 0xffccaa, 0x7e2553, 0x83769c
    }),
    OCTO_CLASSIC("Octo Classic", new int[] {
            0x996600, 0xFFCC00, 0xFF6600, 0x662200,
            0x000000, 0x000000, 0x000000, 0x000000,
            0x000000, 0x000000, 0x000000, 0x000000,
            0x000000, 0x000000, 0x000000, 0x000000
    }),
    LCD("LCD", new int[] {
            0xf2fff2, 0x5b8c7c, 0xadd9bc, 0x0d1a1a,
            0x000000, 0x000000, 0x000000, 0x000000,
            0x000000, 0x000000, 0x000000, 0x000000,
            0x000000, 0x000000, 0x000000, 0x000000
    }),
    C64("Commodore 64", new int[] {
            0x000000, 0xffffff, 0xadadad, 0x626262,
            0xa1683c, 0x9ae29b, 0x887ecb, 0xc9d487,
            0x9f4e44, 0x5cab5e, 0x50459b, 0x6d5412,
            0xcb7e75, 0x6abfc6, 0xa057a3, 0x898989
    }),
    INTELLIVISION("Intellivision", new int[] {
            0x0c0005, 0xfffcff, 0xa7a8a8, 0x3c5800,
            0xff3e00, 0x6ccd30, 0x002dff, 0xfaea27,
            0xffa600, 0x00a720, 0xbd95ff, 0xc9d464,
            0xff3276, 0x5acbff, 0xc81a7d, 0x00780f
    }),
    CGA("CGA", new int[] {
            0x000000, 0xffffff, 0xaaaaaa, 0x555555,
            0xff5555, 0x55ff55, 0x5555ff, 0xffff55,
            0xaa0000, 0x00aa00, 0x0000aa, 0xaa5500,
            0xff55ff, 0x55ffff, 0xaa00aa, 0x00aaaa
    });

    private final String displayName;
    private final int[] rgbColors = new int[16];

    BuiltInChip8Palette(String displayName, int[] colors) {
        this.displayName = displayName;
        for (int i = 0; i < colors.length; i++) {
            this.rgbColors[i] = colors[i] & 0xFFFFFF;
        }
    }

    @Override
    public int getRGB8ForIndex(int colorIndex) {
        return this.rgbColors[colorIndex & 0xF];
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

}
