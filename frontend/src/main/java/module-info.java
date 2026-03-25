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

    exports io.github.arkosammy12.jemu.frontend;
    exports io.github.arkosammy12.jemu.frontend.gui.swing;
    exports io.github.arkosammy12.jemu.frontend.gui.swing.events;
    exports io.github.arkosammy12.jemu.frontend.gui.swing.commands;
    exports io.github.arkosammy12.jemu.frontend.gui.swing.menus;
    exports io.github.arkosammy12.jemu.frontend.audio;

}