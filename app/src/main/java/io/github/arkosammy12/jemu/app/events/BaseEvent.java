package io.github.arkosammy12.jemu.app.events;

import java.util.Objects;

public sealed interface BaseEvent permits Event {

    default boolean isOf(Event other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(this.getClass(), other.getClass());
    }

}
