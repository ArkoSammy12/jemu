package io.github.arkosammy12.jemu.frontend.events.internal;

import io.github.arkosammy12.jemu.frontend.events.MuteEvent;

public record InternalMuteEvent(boolean mute) implements InternalEvent {

    @Override
    public MuteEvent getEvent() {
        return new MuteEvent(this.mute());
    }

}
