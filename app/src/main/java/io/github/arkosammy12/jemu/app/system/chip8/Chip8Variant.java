package io.github.arkosammy12.jemu.app.system.chip8;

import io.github.arkosammy12.jemu.core.chip8.Chip8Host;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum Chip8Variant {
    CHIP_8(
            "CHIP-8",
            "chip8",
                List.of("ch8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_8_VIP, null),
                new Chip8QuirkSet(true,Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
            ),
    STRICT_CHIP_8(
            "STRICT CHIP-8",
            "strict-chip8",
                List.of("ch8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_8_VIP, null),
                new Chip8QuirkSet(true, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
            ),
    CHIP_8X(
            "CHIP-8X",
            "chip8x",
                List.of("c8x"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_8_VIP, null),
                new Chip8QuirkSet(true, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, true, true, false, false, doDisplayWait -> doDisplayWait ? 15 : 11)
            ),
    CHIP_48(
            "CHIP-48",
            "chip48",
                List.of("ch8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, null),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X, true, true, true, true, doDisplayWait -> doDisplayWait ? 15 : 11)
            ),
    SUPER_CHIP_10(
            "SUPER-CHIP 1.0",
            "schip10",
                List.of("ch8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.SCHIP_10_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X, true, true, true, true, _ -> 30)
            ),
    SUPER_CHIP_11(
            "SUPER-CHIP 1.1",
            "schip11",
                List.of("sc11"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.SCHIP_11_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, true, true, true, true, _ -> 30)
            ),
    SUPER_CHIP_MODERN(
            "SUPER-CHIP MODERN",
            "schip-modern",
                List.of("scm"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.OCTO_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, false, true, true, true, _ -> 30)
            ),
    XO_CHIP(
            "XO-CHIP",
            "xochip",
                List.of("xo8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.OCTO_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, false, false, false, false, _ -> 1000)
            ),
    MEGA_CHIP(
            "MEGA-CHIP",
            "megachip",
                List.of("mc8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.MEGACHIP_8_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.NO_INCREMENT, false, false, true, false, _ -> 3000)
            ),
    HYPERWAVE_CHIP_8(
            "HyperWaveCHIP-64",
            "hyperwave-chip64",
                List.of("xo8"),
                new Chip8SpriteFont(Chip8SpriteFont.CHIP_48, Chip8SpriteFont.OCTO_BIG),
                new Chip8QuirkSet(false, Chip8Host.MemoryIncrementQuirk.INCREMENT_BY_X_PLUS_1, false, false, false, false, _ -> 1000)
            ),
    ;

    private final String name;
    private final String id;
    private final List<String> fileExtensions;
    private final Chip8Host.SpriteFont spriteFont;
    private final Chip8QuirkSet defaultQuirkset;

    Chip8Variant(String name, String id, List<String> fileExtensions, Chip8SpriteFont spriteFont, Chip8QuirkSet defaultQuirkset) {
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

    public Chip8QuirkSet getDefaultQuirkset() {
        return this.defaultQuirkset;
    }


}
