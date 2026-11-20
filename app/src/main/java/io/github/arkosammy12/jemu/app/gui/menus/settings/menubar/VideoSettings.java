package io.github.arkosammy12.jemu.app.gui.menus.settings.menubar;

import io.github.arkosammy12.jemu.app.config.settings.VideoSize;
import io.github.arkosammy12.jemu.app.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.ui.VideoSizeChangedEvent;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.gui.MenuBarMenu;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.github.arkosammy12.jemu.app.config.settings.VideoSettings.AspectRatio;

public class VideoSettings extends MenuBarMenu {

    public VideoSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Video");

        JRadioButtonMenuItem useIntegerScalingButton = new JRadioButtonMenuItem("Use integer scaling");
        useIntegerScalingButton.addActionListener(_ -> mainWindow.publishEvent(new UseIntegerScalingSettingChangedEvent(useIntegerScalingButton.isSelected())));
        useIntegerScalingButton.setSelected(mainWindow.getConfig().getPreferenceSettings().getVideoSettings().getUseIntegerScaling());
        mainWindow.onEvent(UseIntegerScalingSettingChangedEvent.class, useIntegerScalingEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getVideoSettings().setUseIntegerScaling(useIntegerScalingEvent.useIntegerScaling());
            useIntegerScalingButton.setSelected(useIntegerScalingEvent.useIntegerScaling());
        });

        JMenu aspectRatioMenu = new JMenu("Aspect Ratio");
        ButtonGroup aspectRatioButtonGroup = new ButtonGroup();
        Map<AspectRatio, JRadioButtonMenuItem> aspectRatioButtonMap = new HashMap<>();
        AspectRatio selectedAspectRatio = mainWindow.getConfig().getPreferenceSettings().getVideoSettings().getAspectRatio();

        JRadioButtonMenuItem autoAspectRatioButton = new JRadioButtonMenuItem(AspectRatio.AUTO.getDisplayName());
        autoAspectRatioButton.addActionListener(_ -> mainWindow.publishEvent(new AspectRatioSettingChangedEvent(AspectRatio.AUTO)));
        aspectRatioButtonGroup.add(autoAspectRatioButton);
        aspectRatioButtonMap.putIfAbsent(AspectRatio.AUTO, autoAspectRatioButton);
        autoAspectRatioButton.setSelected(selectedAspectRatio == AspectRatio.AUTO);

        aspectRatioMenu.add(autoAspectRatioButton);
        aspectRatioMenu.addSeparator();

        Arrays.stream(AspectRatio.values()).filter(aspectRatio -> aspectRatio != AspectRatio.AUTO).forEach(aspectRatio -> {
            JRadioButtonMenuItem aspectRatioButton = new JRadioButtonMenuItem(aspectRatio.getDisplayName());
            aspectRatioButton.addActionListener(_ -> mainWindow.publishEvent(new AspectRatioSettingChangedEvent(aspectRatio)));
            aspectRatioButtonGroup.add(aspectRatioButton);
            aspectRatioMenu.add(aspectRatioButton);
            aspectRatioButtonMap.putIfAbsent(aspectRatio, aspectRatioButton);

            aspectRatioButton.setSelected(selectedAspectRatio == aspectRatio);

        });


        mainWindow.onEvent(AspectRatioSettingChangedEvent.class, aspectRatioSettingChangedEvent -> {
            mainWindow.getConfig().getPreferenceSettings().getVideoSettings().setAspectRatio(aspectRatioSettingChangedEvent.aspectRatio());

            aspectRatioButtonGroup.clearSelection();
            JRadioButtonMenuItem aspectRatioButton = aspectRatioButtonMap.get(aspectRatioSettingChangedEvent.aspectRatio());
            if (aspectRatioButton != null) {
                aspectRatioButton.setSelected(true);
            }
        });

        JMenu videoSizeMenu = new JMenu("Video Size");
        ButtonGroup videoSizeButtonGroup = new ButtonGroup();
        Map<VideoSize, JRadioButtonMenuItem> videoSizeButtonMap = new HashMap<>();
        VideoSize selectedVideoSize = mainWindow.getConfig().getPreferenceSettings().getVideoSettings().getVideoSize().orElse(null);

        for (VideoSize videoSize : VideoSize.values()) {
            JRadioButtonMenuItem videoSizeButton = new JRadioButtonMenuItem(videoSize.getDisplayName());
            videoSizeButton.addActionListener(_ -> {
                mainWindow.getConfig().getPreferenceSettings().getVideoSettings().setVideoSize(videoSize);
                mainWindow.publishEvent(new VideoSizeChangedEvent(videoSize));
            });
            videoSizeButtonGroup.add(videoSizeButton);
            videoSizeMenu.add(videoSizeButton);
            videoSizeButtonMap.put(videoSize, videoSizeButton);

            videoSizeButton.setSelected(selectedVideoSize == videoSize);
        }

        mainWindow.onEvent(VideoSizeChangedEvent.class, videoSizeChangedEvent -> {
            VideoSize newVideoSize = videoSizeChangedEvent.videoSize();
            mainWindow.getConfig().getPreferenceSettings().getVideoSettings().setVideoSize(newVideoSize);
            if (newVideoSize == null) {
                videoSizeButtonGroup.clearSelection();
            } else {
                for (Map.Entry<VideoSize, JRadioButtonMenuItem> videoSizeButtons : videoSizeButtonMap.entrySet()) {
                    videoSizeButtons.getValue().setSelected(newVideoSize == videoSizeButtons.getKey());
                }
            }
        });

        this.getJMenu().add(useIntegerScalingButton);
        this.getJMenu().add(aspectRatioMenu);
        this.getJMenu().add(videoSizeMenu);

    }

}
