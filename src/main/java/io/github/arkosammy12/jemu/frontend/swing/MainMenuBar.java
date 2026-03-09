package io.github.arkosammy12.jemu.frontend.swing;

import io.github.arkosammy12.jemu.frontend.swing.menus.EmulatorMenu;
import io.github.arkosammy12.jemu.frontend.swing.menus.FileMenu;
import io.github.arkosammy12.jemu.frontend.swing.menus.SettingsMenu;

import javax.swing.*;

public class MainMenuBar {

    private final JMenuBar jMenuBar;

    private final FileMenu fileMenu;
    private final EmulatorMenu emulatorMenu;

    public MainMenuBar(MainWindow mainWindow, JFrame jFrame) {

        this.jMenuBar = new JMenuBar();

        this.fileMenu = new FileMenu(mainWindow, jFrame);
        this.emulatorMenu = new EmulatorMenu(mainWindow);
        SettingsMenu settingsMenu = new SettingsMenu(mainWindow);

        this.jMenuBar.add(this.fileMenu.getJMenu());
        this.jMenuBar.add(this.emulatorMenu.getJMenu());
        this.jMenuBar.add(settingsMenu.getJMenu());
    }

    JMenuBar getJMenuBar() {
        return this.jMenuBar;
    }

    public FileMenu getFileMenu() {
        return this.fileMenu;
    }

    public EmulatorMenu getEmulatorMenu() {
        return this.emulatorMenu;
    }

}
