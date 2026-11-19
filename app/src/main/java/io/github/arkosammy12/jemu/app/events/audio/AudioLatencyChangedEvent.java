package io.github.arkosammy12.jemu.app.events.audio;

import io.github.arkosammy12.jemu.app.events.AudioSettingChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.audio.InternalAudioLatencyChangedEvent;

public sealed interface AudioLatencyChangedEvent extends AudioSettingChangedEvent permits InternalAudioLatencyChangedEvent {

    int getLatencyMs();

}
