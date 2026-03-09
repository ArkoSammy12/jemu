package io.github.arkosammy12.jemu.frontend.swing;

import io.github.arkosammy12.jemu.frontend.swing.menus.EmulatorMenu;
import io.github.arkosammy12.jemu.frontend.swing.menus.FileMenu;
import io.github.arkosammy12.jemu.frontend.swing.menus.HelpMenu;
import io.github.arkosammy12.jemu.frontend.swing.menus.SettingsMenu;

import javax.swing.*;

public class MainMenuBar {

    private final JMenuBar jMenuBar;

    private final FileMenu fileMenu;
    private final EmulatorMenu emulatorMenu;
    private final SettingsMenu settingsMenu;
    private final HelpMenu helpMenu;

    public MainMenuBar(MainWindow mainWindow, JFrame jFrame) {

        this.jMenuBar = new JMenuBar();

        this.fileMenu = new FileMenu(mainWindow, jFrame);
        this.emulatorMenu = new EmulatorMenu(mainWindow);
        this.settingsMenu = new SettingsMenu(mainWindow);
        this.helpMenu = new HelpMenu(mainWindow);

        this.jMenuBar.add(this.fileMenu.getJMenu());
        this.jMenuBar.add(this.emulatorMenu.getJMenu());
        this.jMenuBar.add(settingsMenu.getJMenu());
        this.jMenuBar.add(helpMenu.getJMenu());
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

    public HelpMenu getHelpMenu() {
        return this.helpMenu;
    }

    public SettingsMenu getSettingsMenu() {
        return this.settingsMenu;
    }

}
