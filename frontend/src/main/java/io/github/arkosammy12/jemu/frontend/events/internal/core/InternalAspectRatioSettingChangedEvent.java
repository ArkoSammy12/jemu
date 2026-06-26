package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.core.AspectRatioSettingChangedEvent;

public record InternalAspectRatioSettingChangedEvent(VideoSettings.AspectRatio aspectRatio) implements InternalVideoSettingChangedEvent {

    @Override
    public Event getEvent() {
        return new AspectRatioSettingChangedEvent(this.aspectRatio());
    }

}
