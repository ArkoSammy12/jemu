package io.github.arkosammy12.jemu.frontend.events.audio;

import io.github.arkosammy12.jemu.frontend.events.AudioSettingChangeEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.audio.InternalAudioLatencyChangedEvent;

public sealed interface AudioLatencyChangedEvent extends AudioSettingChangeEvent permits InternalAudioLatencyChangedEvent {

    int getLatencyMs();

}
