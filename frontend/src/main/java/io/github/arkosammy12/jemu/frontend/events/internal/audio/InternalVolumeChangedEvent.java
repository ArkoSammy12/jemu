package io.github.arkosammy12.jemu.frontend.events.internal.audio;

import io.github.arkosammy12.jemu.frontend.events.audio.VolumeChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ExposableEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.InternalAudioSettingChangeEvent;

public record InternalVolumeChangedEvent(int newVolume) implements InternalAudioSettingChangeEvent, ExposableEvent {

    @Override
    public VolumeChangedEvent getEvent() {
        return new VolumeChangedEvent(newVolume);
    }

}
