package io.github.arkosammy12.jemu.frontend.events.internal;

import io.github.arkosammy12.jemu.frontend.events.VolumeChangedEvent;

public record InternalVolumeChangedEvent(int newVolume) implements InternalEvent {

    @Override
    public VolumeChangedEvent getEvent() {
        return new VolumeChangedEvent(newVolume);
    }

}
