package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;

public record InternalAspectRatioSettingChangedEvent(VideoSettings.AspectRatio aspectRatio) implements InternalEvent, AspectRatioSettingChangedEvent, ListenableEvent {

    @Override
    public VideoSettings.AspectRatio getAspectRatio() {
        return this.aspectRatio();
    }

}
