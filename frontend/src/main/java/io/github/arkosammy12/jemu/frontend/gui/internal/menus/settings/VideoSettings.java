package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalAspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalSpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;

import javax.swing.*;
import java.util.Arrays;

public class VideoSettings extends MenuBarMenu {

    public VideoSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Video");

        JRadioButtonMenuItem useIntegerScalingButton = new JRadioButtonMenuItem("Use integer scaling");
        useIntegerScalingButton.addActionListener(_ -> {
            boolean useIntegerScaling = useIntegerScalingButton.isSelected();
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setUseIntegerScaling(useIntegerScaling);
            mainWindow.pushEvent(new InternalUseIntegerScalingSettingChangedEvent(useIntegerScaling));
        });

        JMenu aspectRatioMenu = new JMenu("Aspect Ratio");
        ButtonGroup aspectRatioButtonGroup = new ButtonGroup();
        io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio selectedAspectRatio =  mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getAspectRatio();

        JRadioButtonMenuItem autoAspectRatioButton = new JRadioButtonMenuItem(io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.AUTO.getDisplayName());
        autoAspectRatioButton.addActionListener(_ -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setAspectRatio(io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.AUTO);
            mainWindow.pushEvent(new InternalAspectRatioSettingChangedEvent(io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.AUTO));
        });
        aspectRatioButtonGroup.add(autoAspectRatioButton);
        autoAspectRatioButton.setSelected(selectedAspectRatio == io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.AUTO);

        aspectRatioMenu.add(autoAspectRatioButton);
        aspectRatioMenu.addSeparator();

        Arrays.stream(io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.values()).filter(aspectRatio -> aspectRatio != io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio.AUTO).forEach(aspectRatio -> {
            JRadioButtonMenuItem aspectRatioButton = new JRadioButtonMenuItem(aspectRatio.getDisplayName());
            aspectRatioButton.addActionListener(_ -> {
                mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setAspectRatio(aspectRatio);
                mainWindow.pushEvent(new InternalAspectRatioSettingChangedEvent(aspectRatio));
            });
            aspectRatioButtonGroup.add(aspectRatioButton);
            aspectRatioMenu.add(aspectRatioButton);

            aspectRatioButton.setSelected(selectedAspectRatio == aspectRatio);

        });

        this.getJMenu().add(useIntegerScalingButton);
        this.getJMenu().add(aspectRatioMenu);

    }

}
