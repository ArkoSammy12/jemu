module core.test {
    requires org.tinylog.api;
    requires com.google.gson;
    requires org.junit.jupiter.api;
    requires core;

    exports io.github.arkosammy12.jemu.core.test.tests;

    opens io.github.arkosammy12.jemu.core.test.ssts.cdp1802 to com.google.gson;
    opens io.github.arkosammy12.jemu.core.test.ssts.sm83 to com.google.gson;
    opens io.github.arkosammy12.jemu.core.test.ssts.nes6502 to com.google.gson;
}
