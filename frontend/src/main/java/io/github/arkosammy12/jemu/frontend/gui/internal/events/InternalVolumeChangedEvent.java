package io.github.arkosammy12.jemu.frontend.gui.internal.events;

import io.github.arkosammy12.jemu.frontend.gui.swing.events.VolumeChangedEvent;

public record InternalVolumeChangedEvent(int newVolume) implements InternalEvent {

    @Override
    public VolumeChangedEvent getEvent() {
        return new VolumeChangedEvent(newVolume);
    }

}
