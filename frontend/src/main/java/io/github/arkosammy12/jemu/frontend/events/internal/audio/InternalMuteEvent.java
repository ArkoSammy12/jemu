package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.MuteEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;

public record InternalMuteEvent(boolean mute) implements InternalEvent, MuteEvent {

    @Override
    public boolean getMute() {
        return this.mute();
    }

}
