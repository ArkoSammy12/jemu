module frontend {
    requires com.formdev.flatlaf;
    requires com.miglayout.core;
    requires com.miglayout.swing;
    requires java.datatransfer;
    requires java.desktop;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;
    requires com.google.gson;

    exports io.github.arkosammy12.jemu.frontend.gui.swing;
    exports io.github.arkosammy12.jemu.frontend.gui.swing.commands;
    exports io.github.arkosammy12.jemu.frontend.gui.swing.managers;
    exports io.github.arkosammy12.jemu.frontend.events;
    exports io.github.arkosammy12.jemu.frontend.events.audio;
    exports io.github.arkosammy12.jemu.frontend.events.core;
    exports io.github.arkosammy12.jemu.frontend.audio;
    exports io.github.arkosammy12.jemu.frontend.config;
    exports io.github.arkosammy12.jemu.frontend.config.settings;

    opens io.github.arkosammy12.jemu.frontend.config.internal to com.google.gson;
    opens io.github.arkosammy12.jemu.frontend.config.settings.internal to com.google.gson;
    opens io.github.arkosammy12.jemu.frontend.config.state to com.google.gson;

}