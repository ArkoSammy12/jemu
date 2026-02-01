package io.github.arkosammy12.jemu.main;

import com.formdev.flatlaf.util.SystemInfo;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.desktop.QuitStrategy;

public class Main {

    public static final int MAIN_FRAMERATE = 60;

    // Increment this here, in pom.xml and in the version tag in the README.
    public static final String VERSION_STRING = "v0.0.1";

    static void main(String[] args) throws Exception {

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", "jemu");

            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
                desktop.setQuitHandler((_, response) -> response.performQuit());
            }
        }

        System.setProperty("sun.awt.noerasebackground", Boolean.TRUE.toString());
        System.setProperty("flatlaf.uiScale.allowScaleDown", Boolean.TRUE.toString());
        System.setProperty("flatlaf.menuBarEmbedded", Boolean.FALSE.toString());

        Jemu jemu = null;
        try {
            jemu = new Jemu(args);
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