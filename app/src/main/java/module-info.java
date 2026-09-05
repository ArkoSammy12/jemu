module app {
    requires frontend;
    requires info.picocli;
    requires java.datatransfer;
    requires java.desktop;
    requires net.harawata.appdirs;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;
    requires core;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires tools.jackson.dataformat.xml;
    requires com.miglayout.swing;
    requires com.miglayout.core;
    requires jdk.jfr;

    exports io.github.arkosammy12.jemu.app.util to info.picocli;
    exports io.github.arkosammy12.jemu.app.drivers to info.picocli;
    exports io.github.arkosammy12.jemu. app.system.atari2600 to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.nes to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.gameboy to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.cosmacvip to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.rcastudioii to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.chip8 to info.picocli;
    exports io.github.arkosammy12.jemu.app.system.commodore64 to info.picocli;
    exports io.github.arkosammy12.jemu.app.system to info.picocli;

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