package io.github.arkosammy12.jemu.app.gui.menus.settings.panel;

import io.github.arkosammy12.jemu.app.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.app.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.util.EventPublisher;
import io.github.arkosammy12.jemu.app.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.app.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.app.util.settings.panel.EnumPanelSetting;

public class VideoSettingsPanel extends PanelSettingsMenu {

    public VideoSettingsPanel(MainWindow mainWindow, EventPublisher eventPublisher) {
        super(eventPublisher);

        this.addHeader("General");
        BooleanPanelSetting<?> useIntegerScalingSetting = this.addBooleanSetting("Use integer scaling", mainWindow.getConfig().getPreferenceSettings().getVideoSettings().getUseIntegerScaling(), null, null, UseIntegerScalingSettingChangedEvent::new);
        mainWindow.onEvent(UseIntegerScalingSettingChangedEvent.class, useIntegerScalingEvent -> useIntegerScalingSetting.setValue(useIntegerScalingEvent.useIntegerScaling()));

        EnumPanelSetting<VideoSettings.AspectRatio, ?> aspectRatioSetting = this.addEnumSetting("Aspect Ratio", mainWindow.getConfig().getPreferenceSettings().getVideoSettings().getAspectRatio(), null, null, AspectRatioSettingChangedEvent::new);
        mainWindow.onEvent(AspectRatioSettingChangedEvent.class, aspectRatioSettingChangedEvent -> aspectRatioSetting.setValue(aspectRatioSettingChangedEvent.aspectRatio()));
    }

}
