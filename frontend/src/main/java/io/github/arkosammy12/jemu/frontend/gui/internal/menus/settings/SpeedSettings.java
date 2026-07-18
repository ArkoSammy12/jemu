package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalSpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;

import javax.swing.*;
import java.util.Arrays;

public class SpeedSettings extends MenuBarMenu {

    public SpeedSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Speed");

        ButtonGroup speedModeButtonGroup = new ButtonGroup();
        SpeedMode selectedSpeedMode = mainWindow.getConfig().getInternalPreferenceSettings().getInternalSpeedSettings().getSpeedMode();

        JRadioButtonMenuItem normalSpeedModeButton = new JRadioButtonMenuItem(SpeedMode.NORMAL.getDisplayName());
        normalSpeedModeButton.addActionListener(_ -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalSpeedSettings().setSpeedMode(SpeedMode.NORMAL);
            mainWindow.publishEvent(new InternalSpeedModeSettingChangedEvent(SpeedMode.NORMAL));
        });
        speedModeButtonGroup.add(normalSpeedModeButton);
        normalSpeedModeButton.setSelected(selectedSpeedMode == SpeedMode.NORMAL);

        this.getJMenu().add(normalSpeedModeButton);
        this.getJMenu().addSeparator();

        Arrays.stream(SpeedMode.values()).filter(speedMode -> speedMode != SpeedMode.NORMAL).forEach(speedMode -> {
            JRadioButtonMenuItem speedModeButton = new JRadioButtonMenuItem(speedMode.getDisplayName());
            speedModeButton.addActionListener(_ -> {
                mainWindow.getConfig().getInternalPreferenceSettings().getInternalSpeedSettings().setSpeedMode(speedMode);
                mainWindow.publishEvent(new InternalSpeedModeSettingChangedEvent(speedMode));
            });
            speedModeButtonGroup.add(speedModeButton);
            this.getJMenu().add(speedModeButton);

            speedModeButton.setSelected(selectedSpeedMode == speedMode);
        });

    }

}
