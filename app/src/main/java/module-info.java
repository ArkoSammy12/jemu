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

    opens io.github.arkosammy12.jemu.app.io to info.picocli;
    opens io.github.arkosammy12.jemu.app.managers to com.google.gson;
    exports io.github.arkosammy12.jemu.app.util to info.picocli;
    exports io.github.arkosammy12.jemu.app.adapters to info.picocli;
    exports io.github.arkosammy12.jemu.app.drivers to info.picocli;
}