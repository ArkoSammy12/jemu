package io.github.arkosammy12.jemu.app.system.managers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.adapters.Chip8Adapter;
import io.github.arkosammy12.jemu.app.system.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.core.chip8.Chip8Host;
import io.github.arkosammy12.jemu.frontend.util.DisplayNamerProvider;

import java.util.*;
import java.util.function.ToIntFunction;

import static io.github.arkosammy12.jemu.core.common.SystemHost.intToByteArray;

public class Chip8Manager extends SystemManager {

    private static final Category CATEGORY = () -> "CHIP-8";

    private final Variant variant;

    public Chip8Manager(Jemu jemu, SystemRegistry systemRegistry, Variant variant) {
        super(jemu, systemRegistry);
        this.variant = variant;
    }

    @Override
    public SystemAdapter createSystem() throws Exception {
        // TODO: PASSED IN VARIANT CHANGES DEPENDING ON POSSIBLE DATABASE OVERRIDE
        return new Chip8Adapter(this.jemu, this, this.variant);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof Chip8Adapter chip8Adapter && this.variant == chip8Adapter.getVariant();
    }

    @Override
    public String getName() {
        return this.variant.getName();
    }

    @Override
    public String getId() {
        return this.variant.getId();
    }

    @Override
    public Collection<String> getFileExtensions() {
        return this.variant.getFileExtensions();
    }

    @Override
    public Optional<Category> getCategory() {
        return Optional.of(CATEGORY);
    }

    public record QuirkSet(boolean doVFReset, Chip8Host.MemoryIncrementQuirk memoryIncrementQuirk, boolean doDisplayWait, boolean doClipping, boolean doShiftVXInPlace, boolean doJumpWithVX, ToIntFunction<Boolean> instructionsPerFrameSupplier) {}

    public enum Variant {
        CHIP_8(
                "CHIP-8",
                "chip8",
                List.of("ch8"),
                new HexSpriteFont(HexSpriteFont.CHIP_8_VIP, null),
                new QuirkSet(true, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
        ),
        STRICT_CHIP_8(
                "STRICT CHIP-8",
                "strict-chip8",
                List.of("ch8"),
                new HexSpriteFont(HexSpriteFont.CHIP_8_VIP, null),
                new QuirkSet(true, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
        ),
        CHIP_8X(
                "CHIP-8X",
                "chip8x",
                List.of("c8x"),
                new HexSpriteFont(HexSpriteFont.CHIP_8_VIP, null),
                new QuirkSet(true, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
        ),
        CHIP_48(
                "CHIP-48",
                "chip48",
                List.of("ch8"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, null),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X, true, true, true, true, doDisplayWait -> doDisplayWait ? 15 : 11)
        ),
        SUPER_CHIP_10(
                "SUPER-CHIP 1.0",
                "schip10",
                List.of("ch8"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.SCHIP_10_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X, true, true, true, true, _ -> 30)
        ),
        SUPER_CHIP_11(
                "SUPER-CHIP 1.1",
                "schip11",
                List.of("sc11"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.SCHIP_11_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, true, true, true, true, _ -> 30)
        ),
        SUPER_CHIP_MODERN(
                "SUPER-CHIP MODERN",
                "schip-modern",
                List.of("scm"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.OCTO_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, false, true, true, true, _ -> 30)
        ),
        XO_CHIP(
                "XO-CHIP",
                "xochip",
                List.of("xo8"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.OCTO_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, false, false, false, false, _ -> 1000)
        ),
        MEGA_CHIP(
                "MEGA-CHIP",
                "megachip",
                List.of("mc8"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.MEGACHIP_8_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, false, false, true, false, _ -> 3000)
        ),
        HYPERWAVE_CHIP_8(
                "HyperWaveCHIP-64", "hyperwave-chip64",
                List.of("xo8"),
                new HexSpriteFont(HexSpriteFont.CHIP_48, HexSpriteFont.OCTO_BIG),
                new QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, false, false, false, false, _ -> 1000)
        ),
        ;

        private final String name;
        private final String id;
        private final List<String> fileExtensions;
        private final Chip8Host.SpriteFont spriteFont;
        private final QuirkSet defaultQuirkset;

        Variant(String name, String id, List<String> fileExtensions, HexSpriteFont spriteFont, QuirkSet defaultQuirkset) {
            this.name = name;
            this.id = id;
            this.fileExtensions = Collections.unmodifiableList(fileExtensions);
            this.spriteFont = spriteFont;
            this.defaultQuirkset = defaultQuirkset;
        }

        public String getName() {
            return this.name;
        }

        public String getId() {
            return this.id;
        }

        public Collection<String> getFileExtensions() {
            return this.fileExtensions;
        }

        public Chip8Host.SpriteFont getSpriteFont() {
            return this.spriteFont;
        }

        public QuirkSet getDefaultQuirkset() {
            return this.defaultQuirkset;
        }

    }

    public enum BuiltInColorPalette implements Chip8Host.ColorPalette, DisplayNamerProvider {
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

        BuiltInColorPalette(String displayName, int[] colors) {
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

    private record HexSpriteFont(byte[][] smallFont, byte[][] bigFont) implements Chip8Host.SpriteFont {

        public static int SMALL_FONT_BEGIN_OFFSET = 0x50;
        public static int BIG_FONT_BEGIN_OFFSET = 0xA0;

        public static final byte[][] CHIP_8_VIP = intToByteArray(new int[][] {
                {0xF0, 0x90, 0x90, 0x90, 0xF0}, // 0
                {0x60, 0x20, 0x20, 0x20, 0x70}, // 1
                {0xF0, 0x10, 0xF0, 0x80, 0xF0}, // 2
                {0xF0, 0x10, 0xF0, 0x10, 0xF0}, // 3
                {0xA0, 0xA0, 0xF0, 0x20, 0x20}, // 4
                {0xF0, 0x80, 0xF0, 0x10, 0xF0}, // 5
                {0xF0, 0x80, 0xF0, 0x90, 0xF0}, // 6
                {0xF0, 0x10, 0x10, 0x10, 0x10}, // 7
                {0xF0, 0x90, 0xF0, 0x90, 0xF0}, // 8
                {0xF0, 0x90, 0xF0, 0x10, 0xF0}, // 9
                {0xF0, 0x90, 0xF0, 0x90, 0x90}, // A
                {0xF0, 0x50, 0x70, 0x50, 0xF0}, // B
                {0xF0, 0x80, 0x80, 0x80, 0xF0}, // C
                {0xF0, 0x50, 0x50, 0x50, 0xF0}, // D
                {0xF0, 0x80, 0xF0, 0x80, 0xF0}, // E
                {0xF0, 0x80, 0xF0, 0x80, 0x80}  // F
        });

        public static final byte[][] CHIP_48 = intToByteArray(new int[][] {
                {0xF0, 0x90, 0x90, 0x90, 0xF0}, // 0
                {0x20, 0x60, 0x20, 0x20, 0x70}, // 1
                {0xF0, 0x10, 0xF0, 0x80, 0xF0}, // 2
                {0xF0, 0x10, 0xF0, 0x10, 0xF0}, // 3
                {0x90, 0x90, 0xF0, 0x10, 0x10}, // 4
                {0xF0, 0x80, 0xF0, 0x10, 0xF0}, // 5
                {0xF0, 0x80, 0xF0, 0x90, 0xF0}, // 6
                {0xF0, 0x10, 0x20, 0x40, 0x40}, // 7
                {0xF0, 0x90, 0xF0, 0x90, 0xF0}, // 8
                {0xF0, 0x90, 0xF0, 0x10, 0xF0}, // 9
                {0xF0, 0x90, 0xF0, 0x90, 0x90}, // A
                {0xE0, 0x90, 0xE0, 0x90, 0xE0}, // B
                {0xF0, 0x80, 0x80, 0x80, 0xF0}, // C
                {0xE0, 0x90, 0x90, 0x90, 0xE0}, // D
                {0xF0, 0x80, 0xF0, 0x80, 0xF0}, // E
                {0xF0, 0x80, 0xF0, 0x80, 0x80}, // F
        });

        public static final byte[][] SCHIP_10_BIG = intToByteArray(new int[][] {
                {0x3C, 0x7E, 0xFF, 0xC3, 0xC3, 0xC3, 0xC3, 0xFF, 0x7E, 0x3C}, // 0
                {0x18, 0x38, 0x58, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C}, // 1
                {0x3E, 0x7F, 0xC3, 0x06, 0x0C, 0x18, 0x30, 0x60, 0xFF, 0xFF}, // 2
                {0x3C, 0x7E, 0xC3, 0x03, 0x0E, 0x0E, 0x03, 0xC3, 0x7E, 0x3C}, // 3
                {0x06, 0x0E, 0x1E, 0x36, 0x66, 0xC6, 0xFF, 0xFF, 0x06, 0x06}, // 4
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFC, 0xFE, 0x03, 0xC3, 0x7E, 0x3C}, // 5
                {0x3E, 0x7C, 0xE0, 0xC0, 0xFC, 0xFE, 0xC3, 0xC3, 0x7E, 0x3C}, // 6
                {0xFF, 0xFF, 0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x60, 0x60}, // 7
                {0x3C, 0x7E, 0xC3, 0xC3, 0x7E, 0x7E, 0xC3, 0xC3, 0x7E, 0x3C}, // 8
                {0x3C, 0x7E, 0xC3, 0xC3, 0x7F, 0x3F, 0x03, 0x03, 0x3E, 0x7C}, // 9
        });

        public static final byte[][] SCHIP_11_BIG = intToByteArray(new int[][] {
                {0x3C, 0x7E, 0xE7, 0xC3, 0xC3, 0xC3, 0xC3, 0xE7, 0x7E, 0x3C}, // 0
                {0x18, 0x38, 0x58, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C}, // 1
                {0x3E, 0x7F, 0xC3, 0x06, 0x0C, 0x18, 0x30, 0x60, 0xFF, 0xFF}, // 2
                {0x3C, 0x7E, 0xC3, 0x03, 0x0E, 0x0E, 0x03, 0xC3, 0x7E, 0x3C}, // 3
                {0x06, 0x0E, 0x1E, 0x36, 0x66, 0xC6, 0xFF, 0xFF, 0x06, 0x06}, // 4
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFC, 0xFE, 0x03, 0xC3, 0x7E, 0x3C}, // 5
                {0x3E, 0x7C, 0xE0, 0xC0, 0xFC, 0xFE, 0xC3, 0xC3, 0x7E, 0x3C}, // 6
                {0xFF, 0xFF, 0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x60, 0x60}, // 7
                {0x3C, 0x7E, 0xC3, 0xC3, 0x7E, 0x7E, 0xC3, 0xC3, 0x7E, 0x3C}, // 8
                {0x3C, 0x7E, 0xC3, 0xC3, 0x7F, 0x3F, 0x03, 0x03, 0x3E, 0x7C}  // 9
        });

        public static final byte[][] OCTO_BIG = intToByteArray(new int[][] {
                {0xFF, 0xFF, 0xC3, 0xC3, 0xC3, 0xC3, 0xC3, 0xC3, 0xFF, 0xFF}, // 0
                {0x18, 0x78, 0x78, 0x18, 0x18, 0x18, 0x18, 0x18, 0xFF, 0xFF}, // 1
                {0xFF, 0xFF, 0x03, 0x03, 0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF}, // 2
                {0xFF, 0xFF, 0x03, 0x03, 0xFF, 0xFF, 0x03, 0x03, 0xFF, 0xFF}, // 3
                {0xC3, 0xC3, 0xC3, 0xC3, 0xFF, 0xFF, 0x03, 0x03, 0x03, 0x03}, // 4
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF, 0x03, 0x03, 0xFF, 0xFF}, // 5
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF, 0xC3, 0xC3, 0xFF, 0xFF}, // 6
                {0xFF, 0xFF, 0x03, 0x03, 0x06, 0x0C, 0x18, 0x18, 0x18, 0x18}, // 7
                {0xFF, 0xFF, 0xC3, 0xC3, 0xFF, 0xFF, 0xC3, 0xC3, 0xFF, 0xFF}, // 8
                {0xFF, 0xFF, 0xC3, 0xC3, 0xFF, 0xFF, 0x03, 0x03, 0xFF, 0xFF}, // 9
                {0x7E, 0xFF, 0xC3, 0xC3, 0xC3, 0xFF, 0xFF, 0xC3, 0xC3, 0xC3}, // A
                {0xFC, 0xFC, 0xC3, 0xC3, 0xFC, 0xFC, 0xC3, 0xC3, 0xFC, 0xFC}, // B
                {0x3C, 0xFF, 0xC3, 0xC0, 0xC0, 0xC0, 0xC0, 0xC3, 0xFF, 0x3C}, // C
                {0xFC, 0xFE, 0xC3, 0xC3, 0xC3, 0xC3, 0xC3, 0xC3, 0xFE, 0xFC}, // D
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF}, // E
                {0xFF, 0xFF, 0xC0, 0xC0, 0xFF, 0xFF, 0xC0, 0xC0, 0xC0, 0xC0}  // F
        });

        public static final byte[][] MEGACHIP_8_BIG = intToByteArray(new int[][] {
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x18, 0x38, 0x58, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3c}, // 1
                {0x3e, 0x7f, 0xc3, 0x06, 0x0c, 0x18, 0x30, 0x60, 0xff, 0xff}, // 2
                {0x3c, 0x7e, 0xc3, 0x03, 0x0e, 0x0e, 0x03, 0xc3, 0x7e, 0x3c}, // 3
                {0x06, 0x0e, 0x1e, 0x36, 0x66, 0xc6, 0xff, 0xff, 0x06, 0x06}, // 4
                {0xff, 0xff, 0xc0, 0xc0, 0xfc, 0xfe, 0x03, 0xc3, 0x7e, 0x3c}, // 5
                {0x3e, 0x7c, 0xc0, 0xc0, 0xfc, 0xfe, 0xc3, 0xc3, 0x7e, 0x3c}, // 6
                {0xff, 0xff, 0x03, 0x06, 0x0c, 0x18, 0x30, 0x60, 0x60, 0x60}, // 7
                {0x3c, 0x7e, 0xc3, 0xc3, 0x7e, 0x7e, 0xc3, 0xc3, 0x7e, 0x3c}, // 8
                {0x3c, 0x7e, 0xc3, 0xc3, 0x7f, 0x3f, 0x03, 0x03, 0x3e, 0x7c}, // 9
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}, // 0
                {0x3c, 0x7e, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0xc3, 0x7e, 0x3c}  // 0
        });

        public HexSpriteFont {
            smallFont = smallFont != null ? Arrays.stream(smallFont).map(byte[]::clone).toArray(byte[][]::new) : null;
            bigFont = bigFont != null ? Arrays.stream(bigFont).map(byte[]::clone).toArray(byte[][]::new) : null;
        }

        public Optional<byte[][]> getSmallFont() {
            return Optional.ofNullable(this.smallFont);
        }

        public Optional<byte[][]> getBigFont() {
            return Optional.ofNullable(this.bigFont);
        }

        @Override
        public int getSmallFontBeginOffset() {
            return SMALL_FONT_BEGIN_OFFSET;
        }

        @Override
        public int getBigFontBeginOffset() {
            return BIG_FONT_BEGIN_OFFSET;
        }

        public int getSmallFontSpriteOffset(int hex) {
            return SMALL_FONT_BEGIN_OFFSET + (5 * hex);
        }

        public int getBigFontSpriteOffset(int hex) {
            return BIG_FONT_BEGIN_OFFSET + (10 * hex);
        }

    }


}
