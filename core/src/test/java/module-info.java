module core.test {
    requires core;
    requires com.google.gson;
    requires it.unimi.dsi.fastutil;
    requires org.junit.jupiter.api;
    requires org.tinylog.api;

    exports io.github.arkosammy12.jemu.core.test.tests;

    opens io.github.arkosammy12.jemu.core.test.ssts.cdp1802 to com.google.gson;
    opens io.github.arkosammy12.jemu.core.test.ssts.nes6502 to com.google.gson;
    opens io.github.arkosammy12.jemu.core.test.ssts.nmos6502 to com.google.gson;
    opens io.github.arkosammy12.jemu.core.test.ssts.sm83 to com.google.gson;
}
