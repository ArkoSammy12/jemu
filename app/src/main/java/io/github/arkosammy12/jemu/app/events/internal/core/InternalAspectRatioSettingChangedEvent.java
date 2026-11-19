package io.github.arkosammy12.jemu.app.events.internal.core;

import io.github.arkosammy12.jemu.app.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.app.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;

public record InternalAspectRatioSettingChangedEvent(VideoSettings.AspectRatio aspectRatio) implements InternalEvent, AspectRatioSettingChangedEvent, ListenableEvent {

    @Override
    public VideoSettings.AspectRatio getAspectRatio() {
        return this.aspectRatio();
    }

}
