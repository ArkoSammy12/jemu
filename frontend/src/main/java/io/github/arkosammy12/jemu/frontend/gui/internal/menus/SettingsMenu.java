package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.*;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.managers.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SettingsMenu extends MenuBarMenu implements SettingsManager {

    public SettingsMenu(MainWindow mainWindow, JFrame appFrame) {
        this.getJMenu().setText("Settings");
        this.getJMenu().setMnemonic(KeyEvent.VK_S);

        WindowSettings windowSettings = new WindowSettings(mainWindow, appFrame);
        VideoSettings videoSettings = new VideoSettings(mainWindow);
        SoundSettings soundSettings = new SoundSettings(mainWindow);
        SpeedSettings speedSettings = new SpeedSettings(mainWindow);
        EmulationSettings emulationSettings = new EmulationSettings(mainWindow);

        JMenuItem openDataDirectoryButton = new JMenuItem("Open data directory");
        openDataDirectoryButton.addActionListener(_ -> {
            Optional<Path> optionalDataDirectoryPath = mainWindow.getDataDirectoryPath();
            if (optionalDataDirectoryPath.isEmpty()) {
                mainWindow.showDialog("Failed to open data directory", "Data directory path was not specified or failed to be acquired!", MainWindow.DialogType.ERROR);
                return;
            }
            Path dataDirectory = optionalDataDirectoryPath.get();
            if (!Files.exists(dataDirectory) || !Files.isDirectory(dataDirectory)) {
                mainWindow.showDialog("Failed to open data directory", "Directory does not exist: " + dataDirectory, MainWindow.DialogType.ERROR);
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                mainWindow.showDialog("Failed to open data directory", "Desktop API not supported!", MainWindow.DialogType.ERROR);
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(dataDirectory.toFile());
            } catch (IOException e) {
                mainWindow.showDialog("Failed to open data directory", e.getMessage(), MainWindow.DialogType.ERROR);
            }
        });

        this.getJMenu().add(windowSettings.getJMenu());
        this.getJMenu().add(videoSettings.getJMenu());
        this.getJMenu().add(soundSettings.getJMenu());
        this.getJMenu().add(speedSettings.getJMenu());
        this.getJMenu().add(emulationSettings.getJMenu());
        this.getJMenu().addSeparator();
        this.getJMenu().add(openDataDirectoryButton);

    }

}
