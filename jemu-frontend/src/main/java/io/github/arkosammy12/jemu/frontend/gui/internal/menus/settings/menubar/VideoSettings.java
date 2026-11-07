package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.menubar;

import io.github.arkosammy12.jemu.frontend.config.settings.internal.VideoSize;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalAspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.VideoSizeChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings.AspectRatio;

public class VideoSettings extends MenuBarMenu {

    public VideoSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Video");

        JRadioButtonMenuItem useIntegerScalingButton = new JRadioButtonMenuItem("Use integer scaling");
        useIntegerScalingButton.addActionListener(_ -> mainWindow.publishEvent(new InternalUseIntegerScalingSettingChangedEvent(useIntegerScalingButton.isSelected())));
        useIntegerScalingButton.setSelected(mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getUseIntegerScaling());
        mainWindow.onEvent(InternalUseIntegerScalingSettingChangedEvent.class, useIntegerScalingEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setUseIntegerScaling(useIntegerScalingEvent.useIntegerScaling());
            useIntegerScalingButton.setSelected(useIntegerScalingEvent.useIntegerScaling());
        });

        JMenu aspectRatioMenu = new JMenu("Aspect Ratio");
        ButtonGroup aspectRatioButtonGroup = new ButtonGroup();
        Map<AspectRatio, JRadioButtonMenuItem> aspectRatioButtonMap = new HashMap<>();
        AspectRatio selectedAspectRatio =  mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getAspectRatio();

        JRadioButtonMenuItem autoAspectRatioButton = new JRadioButtonMenuItem(AspectRatio.AUTO.getDisplayName());
        autoAspectRatioButton.addActionListener(_ -> mainWindow.publishEvent(new InternalAspectRatioSettingChangedEvent(AspectRatio.AUTO)));
        aspectRatioButtonGroup.add(autoAspectRatioButton);
        aspectRatioButtonMap.putIfAbsent(AspectRatio.AUTO, autoAspectRatioButton);
        autoAspectRatioButton.setSelected(selectedAspectRatio == AspectRatio.AUTO);

        aspectRatioMenu.add(autoAspectRatioButton);
        aspectRatioMenu.addSeparator();

        Arrays.stream(AspectRatio.values()).filter(aspectRatio -> aspectRatio != AspectRatio.AUTO).forEach(aspectRatio -> {
            JRadioButtonMenuItem aspectRatioButton = new JRadioButtonMenuItem(aspectRatio.getDisplayName());
            aspectRatioButton.addActionListener(_ -> mainWindow.publishEvent(new InternalAspectRatioSettingChangedEvent(aspectRatio)));
            aspectRatioButtonGroup.add(aspectRatioButton);
            aspectRatioMenu.add(aspectRatioButton);
            aspectRatioButtonMap.putIfAbsent(aspectRatio, aspectRatioButton);

            aspectRatioButton.setSelected(selectedAspectRatio == aspectRatio);

        });


        mainWindow.onEvent(InternalAspectRatioSettingChangedEvent.class, aspectRatioSettingChangedEvent -> {
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setAspectRatio(aspectRatioSettingChangedEvent.getAspectRatio());

            aspectRatioButtonGroup.clearSelection();
            JRadioButtonMenuItem aspectRatioButton = aspectRatioButtonMap.get(aspectRatioSettingChangedEvent.getAspectRatio());
            if (aspectRatioButton != null) {
                aspectRatioButton.setSelected(true);
            }
        });

        JMenu videoSizeMenu = new JMenu("Video Size");
        ButtonGroup videoSizeButtonGroup = new ButtonGroup();
        Map<VideoSize, JRadioButtonMenuItem> videoSizeButtonMap = new HashMap<>();
        VideoSize selectedVideoSize = mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getVideoSize().orElse(null);

        for (VideoSize videoSize : VideoSize.values()) {
            JRadioButtonMenuItem videoSizeButton = new JRadioButtonMenuItem(videoSize.getDisplayName());
            videoSizeButton.addActionListener(_ -> {
                mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setVideoSize(videoSize);
                mainWindow.publishEvent(new VideoSizeChangedEvent(videoSize));
            });
            videoSizeButtonGroup.add(videoSizeButton);
            videoSizeMenu.add(videoSizeButton);
            videoSizeButtonMap.put(videoSize, videoSizeButton);

            videoSizeButton.setSelected(selectedVideoSize == videoSize);
        }

        mainWindow.onEvent(VideoSizeChangedEvent.class, videoSizeChangedEvent -> {
            VideoSize newVideoSize = videoSizeChangedEvent.videoSize();
            mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().setVideoSize(newVideoSize);
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
