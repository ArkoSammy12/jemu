package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalCoreSettingChangeEvent;

public record InternalSpeedModeSettingChangedEvent(SpeedMode speedMode) implements InternalCoreSettingChangeEvent, ExposableEvent {

    @Override
    public Event getEvent() {
        return new SpeedModeSettingChangedEvent(this.speedMode());
    }

}
