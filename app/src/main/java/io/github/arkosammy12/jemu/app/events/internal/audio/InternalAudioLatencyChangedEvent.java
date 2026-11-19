package io.github.arkosammy12.jemu.app.events.internal.audio;

import io.github.arkosammy12.jemu.app.events.audio.AudioLatencyChangedEvent;
import io.github.arkosammy12.jemu.app.events.internal.InternalEvent;
import io.github.arkosammy12.jemu.app.events.internal.ListenableEvent;

public record InternalAudioLatencyChangedEvent(int latencyMs) implements InternalEvent, ListenableEvent, AudioLatencyChangedEvent {

    @Override
    public int getLatencyMs() {
        return this.latencyMs();
    }

}
