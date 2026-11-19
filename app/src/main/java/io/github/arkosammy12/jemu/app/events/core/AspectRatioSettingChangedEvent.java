package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.app.events.VideoSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;
import io.github.arkosammy12.jemu.app.events.internal.core.InternalAspectRatioSettingChangedEvent;

public sealed interface AspectRatioSettingChangedEvent extends VideoSettingChangedEvent, ListenableEvent permits InternalAspectRatioSettingChangedEvent {

    VideoSettings.AspectRatio getAspectRatio();

}
