package io.github.arkosammy12.jemu.app.events.internal.audio;

import io.github.arkosammy12.jemu.app.events.audio.VolumeChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;

public record InternalVolumeChangedEvent(int newVolume) implements InternalEvent, VolumeChangedEvent, ListenableEvent {

    @Override
    public int getNewVolume() {
        return this.newVolume();
    }

}
