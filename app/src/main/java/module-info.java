module app {
    requires core;
    requires com.formdev.flatlaf;
    requires com.google.gson;
    requires com.miglayout.core;
    requires com.miglayout.swing;
    requires info.picocli;
    requires net.harawata.appdirs;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;
    requires tools.jackson.dataformat.xml;
    requires jdk.jfr;
    requires java.datatransfer;
    requires java.desktop;
    requires java.sql;

    opens io.github.arkosammy12.jemu.app.audio to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config.settings to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config.state to com.google.gson;
    opens io.github.arkosammy12.jemu.app.io to info.picocli;
    opens io.github.arkosammy12.jemu.app.system.atari2600 to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.nes to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.gameboy to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.cosmacvip to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.rcastudioii to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.chip8 to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.chip8.database to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system.commodore64 to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
    opens io.github.arkosammy12.jemu.app.system to com.google.gson, tools.jackson.databind, tools.jackson.dataformat.xml;
}