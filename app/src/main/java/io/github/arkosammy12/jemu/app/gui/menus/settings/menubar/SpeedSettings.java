package io.github.arkosammy12.jemu.app.gui.menus.settings.menubar;

import io.github.arkosammy12.jemu.app.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.app.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.gui.MenuBarMenu;

import javax.swing.*;
import java.util.Arrays;

public class SpeedSettings extends MenuBarMenu {

    public SpeedSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Speed");

        ButtonGroup speedModeButtonGroup = new ButtonGroup();
        SpeedMode selectedSpeedMode = mainWindow.getConfig().getPreferenceSettings().getSpeedSettings().getSpeedMode();

        JRadioButtonMenuItem normalSpeedModeButton = new JRadioButtonMenuItem(SpeedMode.NORMAL.getDisplayName());
        normalSpeedModeButton.addActionListener(_ -> {
            mainWindow.getConfig().getPreferenceSettings().getSpeedSettings().setSpeedMode(SpeedMode.NORMAL);
            mainWindow.publishEvent(new SpeedModeSettingChangedEvent(SpeedMode.NORMAL));
        });
        speedModeButtonGroup.add(normalSpeedModeButton);
        normalSpeedModeButton.setSelected(selectedSpeedMode == SpeedMode.NORMAL);

        this.getJMenu().add(normalSpeedModeButton);
        this.getJMenu().addSeparator();

        Arrays.stream(SpeedMode.values()).filter(speedMode -> speedMode != SpeedMode.NORMAL).forEach(speedMode -> {
            JRadioButtonMenuItem speedModeButton = new JRadioButtonMenuItem(speedMode.getDisplayName());
            speedModeButton.addActionListener(_ -> {
                mainWindow.getConfig().getPreferenceSettings().getSpeedSettings().setSpeedMode(speedMode);
                mainWindow.publishEvent(new SpeedModeSettingChangedEvent(speedMode));
            });
            speedModeButtonGroup.add(speedModeButton);
            this.getJMenu().add(speedModeButton);

            speedModeButton.setSelected(selectedSpeedMode == speedMode);
        });

    }

}
