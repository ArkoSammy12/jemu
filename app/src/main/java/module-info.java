module app {
    requires info.picocli;
    requires java.datatransfer;
    requires java.desktop;
    requires java.sql;
    requires net.harawata.appdirs;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;
    requires core;
    requires org.apache.commons.io;
    requires org.apache.commons.collections4;
    requires com.google.gson;
    requires tools.jackson.dataformat.xml;
    requires com.miglayout.swing;
    requires com.miglayout.core;
    requires com.formdev.flatlaf;
    requires jdk.jfr;

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
    
    opens io.github.arkosammy12.jemu.app.audio to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config.internal to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config.settings.internal to com.google.gson;
    opens io.github.arkosammy12.jemu.app.config.state to com.google.gson;
}