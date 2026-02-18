package io.github.arkosammy12.jemu.frontend.ui;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.util.System;
import io.github.arkosammy12.jemu.application.util.KeyboardLayout;
import io.github.arkosammy12.jemu.frontend.ui.menus.*;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Optional;

public class SettingsBar extends JMenuBar implements EmulatorInitializer {

    private final FileMenu fileMenu;
    private final EmulatorMenu emulatorMenu;
    private final SettingsMenu settingsMenu;

    public SettingsBar(Jemu jemu, MainWindow mainWindow) {
        super();

        this.fileMenu = new FileMenu(jemu, mainWindow);
        this.emulatorMenu = new EmulatorMenu(jemu, mainWindow);
        this.settingsMenu = new SettingsMenu(jemu, mainWindow);
        DebugMenu debugMenu = new DebugMenu(jemu, mainWindow);
        HelpMenu helpMenu = new HelpMenu(mainWindow);

        this.add(fileMenu);
        this.add(emulatorMenu);
        this.add(settingsMenu);
        this.add(debugMenu);
        this.add(helpMenu);
    }

    public void onBreakpoint() {
        this.emulatorMenu.onBreakpoint();
    }

    @Override
    public Optional<byte[]> getRawRom() {
        return this.fileMenu.getRawRom();
    }

    @Override
    public Optional<Path> getRomPath() {
        return this.fileMenu.getRomPath();
    }

    public Optional<KeyboardLayout> getKeyboardLayout() {
        return this.settingsMenu.getKeyboardLayout();
    }

    @Override
    public Optional<System> getSystem() {
        return this.emulatorMenu.getSystem();
    }

}
