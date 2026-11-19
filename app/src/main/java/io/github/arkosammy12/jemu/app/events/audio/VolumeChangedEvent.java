package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.audio.InternalVolumeChangedEvent;

public sealed interface VolumeChangedEvent extends AudioSettingChangedEvent permits InternalVolumeChangedEvent {

    int getNewVolume();

}
