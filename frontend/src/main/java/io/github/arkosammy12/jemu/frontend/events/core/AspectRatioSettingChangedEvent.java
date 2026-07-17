package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalAspectRatioSettingChangedEvent;

public sealed interface AspectRatioSettingChangedEvent extends VideoSettingChangedEvent permits InternalAspectRatioSettingChangedEvent {

    VideoSettings.AspectRatio getAspectRatio();

}
