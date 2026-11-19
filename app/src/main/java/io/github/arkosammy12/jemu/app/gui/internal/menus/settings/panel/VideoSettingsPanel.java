package io.github.arkosammy12.jemu.app.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.app.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.app.events.internal.core.InternalAspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.app.gui.MainWindow;
import io.github.arkosammy12.jemu.app.util.EventPublisher;
import io.github.arkosammy12.jemu.app.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.app.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.app.util.settings.panel.EnumPanelSetting;

public class VideoSettingsPanel extends PanelSettingsMenu {

    public VideoSettingsPanel(MainWindow mainWindow, EventPublisher eventPublisher) {
        super(eventPublisher);

        this.addHeader("General");
        BooleanPanelSetting<?> useIntegerScalingSetting = this.addBooleanSetting("Use integer scaling", mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getUseIntegerScaling(), null, null, InternalUseIntegerScalingSettingChangedEvent::new);
        mainWindow.onEvent(InternalUseIntegerScalingSettingChangedEvent.class, useIntegerScalingEvent -> useIntegerScalingSetting.setValue(useIntegerScalingEvent.useIntegerScaling()));

        EnumPanelSetting<VideoSettings.AspectRatio, ?> aspectRatioSetting = this.addEnumSetting("Aspect Ratio", mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getAspectRatio(), null, null, InternalAspectRatioSettingChangedEvent::new);
        mainWindow.onEvent(InternalAspectRatioSettingChangedEvent.class, aspectRatioSettingChangedEvent -> aspectRatioSetting.setValue(aspectRatioSettingChangedEvent.getAspectRatio()));
    }

}
