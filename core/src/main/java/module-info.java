module core {
    requires it.unimi.dsi.fastutil;
    requires org.apache.commons.io;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;

    exports io.github.arkosammy12.jemu.core.common;
    exports io.github.arkosammy12.jemu.core.cosmacvip;
    exports io.github.arkosammy12.jemu.core.studioii;
    exports io.github.arkosammy12.jemu.core.hardware;
    exports io.github.arkosammy12.jemu.core.drivers;
    exports io.github.arkosammy12.jemu.core.exceptions;
    exports io.github.arkosammy12.jemu.core.gameboy;
    exports io.github.arkosammy12.jemu.core.gameboycolor;
    exports io.github.arkosammy12.jemu.core.nes;
    exports io.github.arkosammy12.jemu.core.nes.ines;
    exports io.github.arkosammy12.jemu.core.nes.mappers;
    exports io.github.arkosammy12.jemu.core.gameboy.mbcs;
    exports io.github.arkosammy12.jemu.core.atari2600;
    exports io.github.arkosammy12.jemu.core.chip8;
    exports io.github.arkosammy12.jemu.core.chip8.audio;
    exports io.github.arkosammy12.jemu.core.chip8.bus;
    exports io.github.arkosammy12.jemu.core.chip8.display;
    exports io.github.arkosammy12.jemu.core.chip8.interpreters;
    exports io.github.arkosammy12.jemu.core.commodore64;
    exports io.github.arkosammy12.jemu.core.commodore64.crt;

    exports io.github.arkosammy12.jemu.core.util;

}