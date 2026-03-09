package io.github.arkosammy12.jemu.app;

import org.tinylog.Logger;

public class Main {

    // Increment this here, in pom.xml and in the version tag in the README.
    public static final String VERSION_STRING = "v0.0.1";

    static void main() throws Exception {
        Jemu jemu = null;
        try {
            jemu = new Jemu();
            jemu.start();
        } catch (Throwable t) {
            Logger.error("jemu has crashed! {}", t);
        } finally {
            if (jemu != null) {
                jemu.onShutdown();
            }
        }
    }
}