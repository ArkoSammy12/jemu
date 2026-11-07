package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.core.InternalAspectRatioSettingChangedEvent;

public sealed interface AspectRatioSettingChangedEvent extends VideoSettingChangedEvent, ListenableEvent permits InternalAspectRatioSettingChangedEvent {

    VideoSettings.AspectRatio getAspectRatio();

}
