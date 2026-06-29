package io.github.arkosammy12.jemu.frontend.events.internal.core;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.events.core.UseIntegerScalingSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;

public record InternalUseIntegerScalingSettingChangedEvent(boolean useIntegerScaling) implements InternalVideoSettingChangedEvent, ExposableEvent {

    @Override
    public Event getEvent() {
        return new UseIntegerScalingSettingChangedEvent(this.useIntegerScaling());
    }

}
