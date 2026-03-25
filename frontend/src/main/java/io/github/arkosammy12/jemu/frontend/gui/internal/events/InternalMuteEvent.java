package io.github.arkosammy12.jemu.frontend.gui.internal.events;

import io.github.arkosammy12.jemu.frontend.gui.swing.events.MuteEvent;

public record InternalMuteEvent(boolean mute) implements InternalEvent {

    @Override
    public MuteEvent getEvent() {
        return new MuteEvent(this.mute());
    }

}
