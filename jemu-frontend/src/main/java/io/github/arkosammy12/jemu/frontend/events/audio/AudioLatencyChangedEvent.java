package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalAudioLatencyChangedEvent;

public sealed interface AudioLatencyChangedEvent extends AudioSettingChangedEvent permits InternalAudioLatencyChangedEvent {

    int getLatencyMs();

}
