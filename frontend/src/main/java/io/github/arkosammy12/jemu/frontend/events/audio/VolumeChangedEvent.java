package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalVolumeChangedEvent;

public sealed interface VolumeChangedEvent extends AudioSettingChangeEvent permits InternalVolumeChangedEvent {

    int getNewVolume();

}
