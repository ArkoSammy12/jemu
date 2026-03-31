module core {
    requires it.unimi.dsi.fastutil;
    requires org.apache.commons.io;
    requires org.jctools.core;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;

    exports io.github.arkosammy12.jemu.core.common;
    exports io.github.arkosammy12.jemu.core.cosmacvip;
    exports io.github.arkosammy12.jemu.core.cpu;
    exports io.github.arkosammy12.jemu.core.drivers;
    exports io.github.arkosammy12.jemu.core.exceptions;
    exports io.github.arkosammy12.jemu.core.gameboy;
    exports io.github.arkosammy12.jemu.core.gameboycolor;
    exports io.github.arkosammy12.jemu.core.nes;
    exports io.github.arkosammy12.jemu.core.nes.ines;

}