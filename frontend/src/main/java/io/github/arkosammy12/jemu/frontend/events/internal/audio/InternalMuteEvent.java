package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ListenableEvent;

public record InternalMuteEvent(boolean mute) implements InternalEvent, MuteEvent, ListenableEvent {

    @Override
    public boolean getMute() {
        return this.mute();
    }

}
