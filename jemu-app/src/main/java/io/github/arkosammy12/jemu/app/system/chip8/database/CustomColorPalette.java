package io.github.arkosammy12.jemu.app.system.chip8.database;

import io.github.arkosammy12.jemu.core.chip8.Chip8Host;

class CustomColorPalette implements Chip8Host.ColorPalette {

    private final int[] argbColors = new int[16];

    public CustomColorPalette(Chip8Host.ColorPalette base, int[][] customPixelColors) {
        for (int i = 0; i < 16; i++) {
            this.argbColors[i] = base.getRGB8ForIndex(i);
        }
        if (customPixelColors == null) {
            return;
        }
        for (int i = 0; i < customPixelColors.length && i < 16; i++) {
            int r = customPixelColors[i][0];
            int g = customPixelColors[i][1];
            int b = customPixelColors[i][2];
            this.argbColors[i] = (r << 16) | (g << 8) | b;
        }
    }

    @Override
    public int getRGB8ForIndex(int colorIndex) {
        return this.argbColors[colorIndex];
    }

}