package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalVolumeChangedEvent;

public sealed interface VolumeChangedEvent extends AudioSettingChangedEvent permits InternalVolumeChangedEvent {

    int getNewVolume();

}
