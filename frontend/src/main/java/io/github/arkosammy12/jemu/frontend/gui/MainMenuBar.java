package io.github.arkosammy12.jemu.frontend.gui;

import io.github.arkosammy12.jemu.frontend.gui.internal.menus.EmulatorMenu;
import io.github.arkosammy12.jemu.frontend.gui.internal.menus.FileMenu;
import io.github.arkosammy12.jemu.frontend.gui.internal.menus.HelpMenu;
import io.github.arkosammy12.jemu.frontend.gui.internal.menus.SettingsMenu;
import org.jetbrains.annotations.ApiStatus;

import javax.swing.*;

public class MainMenuBar {

    private final JMenuBar jMenuBar;

    private final FileMenu fileMenu;
    private final EmulatorMenu emulatorMenu;
    private final SettingsMenu settingsMenu;
    private final HelpMenu helpMenu;

    public MainMenuBar(MainWindow mainWindow, JFrame appFrame) {
        this.jMenuBar = new JMenuBar();

        this.fileMenu = new FileMenu(mainWindow, appFrame);
        this.emulatorMenu = new EmulatorMenu(mainWindow);
        this.settingsMenu = new SettingsMenu(mainWindow, appFrame);
        this.helpMenu = new HelpMenu(mainWindow, appFrame);

        this.jMenuBar.add(this.fileMenu.getJMenu());
        this.jMenuBar.add(this.emulatorMenu.getJMenu());
        this.jMenuBar.add(settingsMenu.getJMenu());
        this.jMenuBar.add(helpMenu.getJMenu());
    }

    JMenuBar getJMenuBar() {
        return this.jMenuBar;
    }

    @ApiStatus.Internal
    public FileMenu getFileMenu() {
        return this.fileMenu;
    }

    @ApiStatus.Internal
    public EmulatorMenu getEmulatorMenu() {
        return this.emulatorMenu;
    }

    @ApiStatus.Internal
    public HelpMenu getHelpMenu() {
        return this.helpMenu;
    }

}
