package io.github.arkosammy12.jemu.app;

import org.tinylog.Logger;

public class Main {

    static void main(String[] args) {
        try {
            Jemu jemu = new Jemu(args);
            jemu.start();
        } catch (Throwable t) {
            Logger.error(t, "jemu has crashed!");
        }
    }

}