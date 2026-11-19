package io.github.arkosammy12.jemu.app.events;

import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;

import java.util.Objects;

public sealed interface BaseEvent permits Event, InternalEvent {

    default boolean isOf(Event other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(this.getClass(), other.getClass());
    }

}
