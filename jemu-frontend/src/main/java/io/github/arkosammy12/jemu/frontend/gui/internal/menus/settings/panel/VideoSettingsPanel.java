package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalAspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalUseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.PanelSettingsMenu;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.BooleanPanelSetting;
import io.github.arkosammy12.jemu.frontend.util.settings.panel.EnumPanelSetting;

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
