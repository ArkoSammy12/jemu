package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.config.settings.SpeedMode;
import io.github.arkosammy12.jemu.frontend.events.core.SpeedModeSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;

public record InternalSpeedModeSettingChangedEvent(SpeedMode speedMode) implements InternalEvent, SpeedModeSettingChangedEvent {

    @Override
    public SpeedMode getSpeedMode() {
        return this.speedMode();
    }

}
