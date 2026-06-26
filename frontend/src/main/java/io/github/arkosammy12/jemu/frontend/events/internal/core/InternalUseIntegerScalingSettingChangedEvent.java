package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.core.UseIntegerScalingSettingChangedEvent;

public record InternalUseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements InternalVideoSettingChangedEvent {

    @Override
    public Event getEvent() {
        return new UseIntegerScalingSettingChangedEvent(this.useIntegerScaling());
    }

}
