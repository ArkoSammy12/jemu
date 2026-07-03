package io.github.arkosammy12.jemu.frontend.events.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;

public interface ExposableEvent extends InternalEvent {

    Event getEvent();

}
