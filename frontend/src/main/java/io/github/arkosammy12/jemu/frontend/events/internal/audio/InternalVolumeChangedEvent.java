package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.VolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalEvent;

public record InternalVolumeChangedEvent(int newVolume) implements InternalEvent, VolumeChangedEvent {

    @Override
    public int getNewVolume() {
        return this.newVolume();
    }

}
