package io.github.arkosammy12.jemu.app.events.internal.core;

import io.github.arkosammy12.jemu.app.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.app.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;

public record InternalSpeedModeSettingChangedEvent(SpeedMode speedMode) implements InternalEvent, SpeedModeSettingChangedEvent {

    @Override
    public SpeedMode getSpeedMode() {
        return this.speedMode();
    }

}
